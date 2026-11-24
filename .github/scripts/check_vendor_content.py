#!/usr/bin/env python3
import hashlib
import re
import struct
import subprocess
import sys
from pathlib import Path

SCOPE = ("patches/src/main/", "extensions/")
TEST_SOURCES = "/src/test/"
SOURCE_SUFFIXES = (".kt", ".java")
ALLOWLIST = ".github/vendor-content-allowlist.txt"
PAIRIP_RESOURCES = "patches/src/main/resources/pairip/"
PAIRIP_TEXT_SUFFIXES = (".tsv", ".stubs")
MAX_BYTES = 65536
MAX_SMALI_LINES = 150
PRINTABLE_RUN = 64
KEY_MAGIC = b"MKEY\x01"
KEY_BLOCK = 1704
BINARY_MAGICS = {
    b"dex\n": "dex",
    b"\x7fELF": "ELF",
    b"PK\x03\x04": "zip",
    b"MDLT": "delta",
    b"\x00IAP\x02": "pairip program",
    b"\x28\xb5\x2f\xfd": "zstd",
    b"\x1f\x8b": "gzip",
    b"SQLite format 3\x00": "sqlite",
    b"\x02\x00\x0c\x00": "resources.arsc",
}
RESTORED_CODE_MAGICS = (b"dex\n", b"\x7fELF")
ENCODED_RUN = re.compile(r"[A-Za-z0-9+/]{4096,}={0,2}|[0-9a-fA-F]{4096,}")
SMALI_LINE = re.compile(
    r"^\s*(?:invoke-(?:virtual|direct|static|interface|super|polymorphic|custom)(?:/range)?|const(?:-\w+|/\w+)?|"
    r"move(?:-\w+|/\w+)*|[is](?:get|put)(?:-\w+)?|a(?:get|put)(?:-\w+)?|if-\w+|goto(?:/\d+)?|return(?:-\w+)?|"
    r"new-(?:instance|array)|check-cast|instance-of|array-length|throw|monitor-(?:enter|exit)|"
    r"(?:add|sub|mul|div|rem|and|or|xor|shl|shr|ushr|neg|not)-\w+|(?:int|long|float|double)-to-\w+|cmp\w*-\w+)\b.*$",
    re.M,
)
PINNED_LINE = re.compile(r"^([0-9a-f]{64})  (\S.*)$")


class Tree:
    def __init__(self, staged):
        self.staged = staged

    def read(self, path):
        if self.staged:
            result = subprocess.run(["git", "show", f":{path}"], capture_output=True)
            return result.stdout if result.returncode == 0 else None
        file = Path(path)
        return file.read_bytes() if file.is_file() else None

    def changed(self):
        if self.staged:
            command = ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR", "--", *SCOPE]
        else:
            command = ["git", "ls-files", "--", *SCOPE]
        paths = subprocess.run(command, capture_output=True, text=True, check=True).stdout.split("\n")[:-1]
        return [path for path in paths if TEST_SOURCES not in path]


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def load_pinned(tree):
    pinned, failures = {}, []
    allowlist = tree.read(ALLOWLIST)
    for line in (allowlist or b"").decode().split("\n"):
        if not line.strip():
            continue
        match = PINNED_LINE.match(line)
        if match:
            pinned[match.group(2)] = match.group(1)
        else:
            failures.append(f"{ALLOWLIST}: malformed line: {line}")
    return pinned, failures


def parse_key(data):
    if not data.startswith(KEY_MAGIC):
        raise ValueError("no key magic")
    at = len(KEY_MAGIC) + 64
    material = []

    def take(fmt):
        nonlocal at
        values = struct.unpack_from(fmt, data, at)
        at += struct.calcsize(fmt)
        return values

    for _ in range(take("<I")[0]):
        offset, length = take("<II")
        for _ in range((length + KEY_BLOCK - 1) // KEY_BLOCK):
            (period,) = take("<H")
            material.append(data[at:at + period])
            at += period
    for _ in range(take("<I")[0]):
        _, length, kind = take("<IIB")
        if kind == 0:
            material.append(data[at:at + length])
            at += length
        elif kind == 2:
            take("<I")
        elif kind != 1:
            raise ValueError("unknown record kind")
    for _ in range(take("<I")[0]):
        (size,) = take("<QB")[1:]
        at += size
    if at != len(data):
        raise ValueError("trailing bytes")
    return material


def longest_printable_run(data):
    longest = run = 0
    for byte in data:
        run = run + 1 if 0x20 <= byte <= 0x7E else 0
        longest = max(longest, run)
    return longest


def check_key(path, data):
    try:
        material = parse_key(data)
    except (ValueError, struct.error) as error:
        return [f"{path}: V5 key file: not an approved key format ({error})"]
    failures = []
    if any(magic in chunk for chunk in material for magic in RESTORED_CODE_MAGICS):
        failures.append(f"{path}: V6 restored code: a key record holds a dex or ELF header")
    if any(longest_printable_run(chunk) >= PRINTABLE_RUN for chunk in material):
        failures.append(f"{path}: V6 restored code: a key record holds {PRINTABLE_RUN} printable bytes in a row")
    return failures


def binary_kind(data):
    for magic, kind in BINARY_MAGICS.items():
        if data.startswith(magic):
            return kind
    try:
        data.decode("utf-8")
    except UnicodeDecodeError:
        return "non-UTF-8 data"
    return None


def smali_lines(text):
    return len(SMALI_LINE.findall(text))


def check_file(path, data, pinned):
    if path in pinned:
        if sha256(data) != pinned[path]:
            return [f"{path}: changed since it was registered in {ALLOWLIST}"]
        return []

    if path.startswith(PAIRIP_RESOURCES):
        if path.endswith(PAIRIP_TEXT_SUFFIXES):
            return []
        if path.endswith(".bin") and path.rsplit("/", 1)[1].startswith("key-"):
            return check_key(path, data)
        return [f"{path}: V5 key file: not one of the approved formats"]

    failures = []
    kind = binary_kind(data)
    if kind:
        failures.append(f"{path}: V1 binary: {kind} not registered in {ALLOWLIST}")
    if path.endswith(".smali"):
        failures.append(f"{path}: V2 smali: not registered in {ALLOWLIST}")
    if len(data) > MAX_BYTES and not path.endswith(SOURCE_SUFFIXES):
        failures.append(f"{path}: V3 size: {len(data)} bytes, over {MAX_BYTES}")
    if not kind:
        text = data.decode("utf-8")
        if ENCODED_RUN.search(text):
            failures.append(f"{path}: V4 encoded blob: a run of 4096 or more base64 or hex characters")
        if path.endswith(SOURCE_SUFFIXES) and smali_lines(text) > MAX_SMALI_LINES:
            failures.append(f"{path}: V7 inline smali: {smali_lines(text)} instruction lines in string literals")
    return failures


def check(tree):
    pinned, failures = load_pinned(tree)
    for path in tree.changed():
        data = tree.read(path)
        if data is not None:
            failures += check_file(path, data, pinned)
    return failures


def self_test():
    plain = {}
    dex = b"dex\n035\x00" + bytes(56)
    covered = {"extensions/x/a.dex": sha256(dex)}
    key_header = KEY_MAGIC + bytes(64) + struct.pack("<I", 0)
    clean_key = key_header + struct.pack("<I", 1) + struct.pack("<IIB", 0, 4, 0) + b"\x01\x02\x03\x04" + struct.pack("<I", 0)
    code_key = key_header + struct.pack("<I", 1) + struct.pack("<IIB", 0, 4, 0) + b"\x7fELF" + struct.pack("<I", 0)
    text_key = key_header + struct.pack("<I", 1) + struct.pack("<IIB", 0, 64, 0) + b"a" * 64 + struct.pack("<I", 0)
    smali = "".join("    invoke-static {v0}, Lx;->y()V\n" for _ in range(MAX_SMALI_LINES + 1))
    refused = [
        ("patches/src/main/resources/a/x.bin", b"dex\n035\x00" + bytes(56), plain),
        ("patches/src/main/resources/a/y.bin", b"\x7fELF" + bytes(60), plain),
        ("patches/src/main/resources/a/z.bin", b"MDLT\x01" + bytes(59), plain),
        ("patches/src/main/resources/a/w.bin", b"\x00IAP\x02" + bytes(59), plain),
        ("patches/src/main/resources/a/big.txt", b"a" * 70000, plain),
        ("patches/src/main/resources/a/x.smali", b".class public Lx;\n", plain),
        ("patches/src/main/resources/a/blob.txt", b"A" * 5000, plain),
        ("patches/src/main/kotlin/a/B.kt", f'val s = """\n{smali}"""\n'.encode(), plain),
        (PAIRIP_RESOURCES + "key-arm64-v8a-x-0123456789abcdef.bin", code_key, plain),
        (PAIRIP_RESOURCES + "key-arm64-v8a-y-0123456789abcdef.bin", text_key, plain),
        (PAIRIP_RESOURCES + "delta-arm64-v8a-x-0123456789abcdef.bin", b"MDLT\x01" + bytes(59), plain),
        ("extensions/x/a.dex", dex + b"\x00", covered),
    ]
    accepted = [
        ("patches/src/main/kotlin/a/C.kt", b"val a = 1\n" * 10, plain),
        ("extensions/x/a.dex", dex, covered),
        (PAIRIP_RESOURCES + "a-1.stubs", b"Lx;->y()V\n", plain),
        (PAIRIP_RESOURCES + "key-arm64-v8a-z-0123456789abcdef.bin", clean_key, plain),
        (PAIRIP_RESOURCES + "a-1.tsv", b"string\tA\tb\tc\n" * 10, plain),
    ]
    wrong = [path for path, data, pins in refused if not check_file(path, data, pins)]
    wrong += [path for path, data, pins in accepted if check_file(path, data, pins)]
    if wrong:
        sys.exit(f"self-test failed: {wrong}")


def main():
    self_test()
    failures = check(Tree("--staged" in sys.argv))
    if failures:
        print("Vendor content found in the bundle sources:", file=sys.stderr)
        print("\n".join(failures), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
