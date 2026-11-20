#!/usr/bin/env python3

import struct
import sys
import tempfile
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_banner import check_credits


def chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))


PNG = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
       + chunk(b"IDAT", zlib.compress(b"\x00\x00\x00\x00\x00")) + chunk(b"IEND", b""))
PICTURES = ["top/pointing.png", "bottom/chibi-staff.png", "icons/installing.png"]


def aqua_tree(directory, pictures, credited):
    aqua = Path(directory) / "aqua"
    for picture in pictures:
        (aqua / picture).parent.mkdir(parents=True, exist_ok=True)
        (aqua / picture).write_bytes(PNG)
    rows = "".join(f"| `{path}` | fan art |\n" for path in credited)
    (aqua / "CREDITS.md").write_text(f"# Aqua pictures\n\n| File | Source |\n|---|---|\n{rows}")
    return aqua


def refusal(aqua):
    try:
        check_credits(aqua)
    except SystemExit as error:
        return str(error)
    return None


with tempfile.TemporaryDirectory() as temp:
    assert refusal(aqua_tree(temp, PICTURES, PICTURES)) is None

with tempfile.TemporaryDirectory() as temp:
    message = refusal(aqua_tree(temp, [*PICTURES, "top/new.png"], PICTURES))
    assert message and "no credit in CREDITS.md: top/new.png" in message

with tempfile.TemporaryDirectory() as temp:
    message = refusal(aqua_tree(temp, PICTURES[1:], PICTURES))
    assert message and "credited but missing: top/pointing.png" in message

with tempfile.TemporaryDirectory() as temp:
    aqua = aqua_tree(temp, PICTURES, PICTURES)
    (aqua / "bottom/chibi-staff.png").write_bytes(b"not a png")
    message = refusal(aqua)
    assert message and "is not a PNG" in message

with tempfile.TemporaryDirectory() as temp:
    aqua = aqua_tree(temp, PICTURES, PICTURES)
    (aqua / "bottom/chibi-staff.png").write_bytes(PNG[:40])
    message = refusal(aqua)
    assert message and "chibi-staff.png" in message and ("truncated" in message or "damaged" in message), message

with tempfile.TemporaryDirectory() as temp:
    message = refusal(aqua_tree(temp, PICTURES[:2], PICTURES[:2]))
    assert message and "required picture missing: icons/installing.png" in message

print("PASS banner pictures match CREDITS.md and are PNGs")
