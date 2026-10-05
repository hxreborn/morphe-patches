#!/usr/bin/env python3
import re
import subprocess
import sys
from pathlib import Path

SOURCE_ROOTS = ("patches/src/main", "extensions")
SOURCE_SUFFIXES = (".kt", ".java")
UNSUPPORTED_FLAGS = re.compile(r"\b(?:CANON_EQ|UNICODE_CHARACTER_CLASS)\b")
QUANTIFIER = re.compile(r"\{\d+(?:,\d*)?\}")
UNBOUNDED_QUANTIFIER = re.compile(r"\{\d+,\}")
RAW_LITERAL = re.compile(r'"""(.*?)"""(?!")', re.S)
PLAIN_LITERAL = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
KOTLIN_REGEX_CALL = re.compile(r"\b(?:Regex|Pattern\.compile)\(\s*|\.matches\(\s*(?=\")")
JAVA_REGEX_CALL = re.compile(r"\bPattern\.compile\(\s*|\.(?:matches|replaceAll|replaceFirst|split)\(\s*(?=\")")
TO_REGEX = re.compile(r'("""(?:.*?)"""|"(?:[^"\\\n]|\\.)*")\s*\.toRegex\(', re.S)
TEMPLATE = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_]\w*")
KOTLIN_ESCAPE = re.compile(r"\\(u[0-9a-fA-F]{4}|.)")


def unescape_plain(body):
    simple = {"t": "\t", "n": "\n", "r": "\r", "b": "\b", "\\": "\\", '"': '"', "'": "'", "$": "\x00"}
    return KOTLIN_ESCAPE.sub(lambda m: chr(int(m[1][1:], 16)) if m[1][0] == "u" else simple.get(m[1], "\\" + m[1]), body)


def pattern_text(literal, java):
    if java:
        return unescape_plain(literal[3:-3] if literal.startswith('"""') else literal[1:-1]).replace("\x00", "$")
    if literal.startswith('"""'):
        body = literal[3:-3].replace("${'$'}", "\x00")
    else:
        body = unescape_plain(literal[1:-1])
    return TEMPLATE.sub("X", body).replace("\x00", "$")


def regex_literals(source, java=False):
    for call in (JAVA_REGEX_CALL if java else KOTLIN_REGEX_CALL).finditer(source):
        rest = source[call.end():]
        match = RAW_LITERAL.match(rest) or PLAIN_LITERAL.match(rest)
        if match:
            yield call.end(), pattern_text(match[0], java)
    if not java:
        for match in TO_REGEX.finditer(source):
            yield match.start(), pattern_text(match[1], java)


def problems(pattern):
    found = []
    in_class = 0
    lookbehind = []
    depth = 0
    i = 0
    while i < len(pattern):
        c = pattern[i]
        if c == "\\":
            if pattern.startswith("Q", i + 1):
                end = pattern.find("\\E", i + 2)
                i = len(pattern) if end < 0 else end + 2
                continue
            if pattern.startswith("b{", i + 1) and not in_class:
                found.append(r"\b{...} is not supported")
            if pattern[i + 1:i + 3] in ("p{", "P{", "x{", "N{"):
                end = pattern.find("}", i + 3)
                i = len(pattern) if end < 0 else end + 1
                continue
            i += 2
            continue
        if in_class:
            if c == "[":
                in_class += 1
            elif c == "]":
                in_class -= 1
            i += 1
            continue
        if c == "[":
            in_class = 1
            i += 1
            if pattern.startswith("^", i):
                i += 1
            if pattern.startswith("]", i):
                i += 1
            continue
        if c == "(":
            depth += 1
            if pattern.startswith("?<=", i + 1) or pattern.startswith("?<!", i + 1):
                lookbehind.append(depth)
            flags = re.match(r"\(\?([a-zA-Z-]*)[:)]", pattern[i:])
            if flags and "U" in flags[1]:
                found.append("the (?U) flag is not supported")
        elif c == ")":
            if lookbehind and lookbehind[-1] == depth:
                lookbehind.pop()
            depth -= 1
        elif c == "{":
            quantifier = QUANTIFIER.match(pattern, i)
            if quantifier:
                if lookbehind and UNBOUNDED_QUANTIFIER.fullmatch(quantifier[0]):
                    found.append("lookbehind must have a bounded length")
                i = quantifier.end()
                continue
        elif c == "}":
            found.append("unescaped } must be written \\}")
        elif c in "*+" and lookbehind and not (c == "+" and i and pattern[i - 1] in "*+?}"):
            found.append("lookbehind must have a bounded length")
        i += 1
    return found


def staged_sources():
    names = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR", "--", *SOURCE_ROOTS],
        capture_output=True, text=True, check=True,
    ).stdout.split()
    for name in filter(is_shipped_source, names):
        yield name, subprocess.run(["git", "show", f":{name}"], capture_output=True, text=True, check=True).stdout


def is_shipped_source(name):
    return name.endswith(SOURCE_SUFFIXES) and not {"test", "build"} & set(Path(name).parts)


def tree_sources():
    for root in SOURCE_ROOTS:
        for path in sorted(Path(root).rglob("*")):
            if is_shipped_source(str(path)):
                yield str(path), path.read_text()


def check(sources):
    failures = []
    for name, source in sources:
        for match in UNSUPPORTED_FLAGS.finditer(source):
            line = source.count("\n", 0, match.start()) + 1
            failures.append(f"{name}:{line}: {match[0]} throws IllegalArgumentException on Android")
        for offset, pattern in regex_literals(source, name.endswith(".java")):
            line = source.count("\n", 0, offset) + 1
            failures += [f"{name}:{line}: {problem}: {pattern}" for problem in dict.fromkeys(problems(pattern))]
    return failures


def self_test():
    rejected = ["a}", "[^}]*}", r"\b{g}", r"(?U)\w", "(?<=a+)b", "(?<=a*)b", "(?<=a{2,})b"]
    accepted = [r"\}", "[}]", "x{2}", "a{1,3}", r"\p{Lu}", r"\x{41}", r"\Qa}\E", r"(?<n>a)\k<n>", "(?<=a{1,3})b",
                r"(?<![\w$])x", r"([@#])\{([A-Za-z][A-Za-z0-9]*)\}", r'"\./[^"]+"\([\w$,]*\)\{', "a++", "(?i)a", "[]}]"]
    wrong = [p for p in rejected if not problems(p)] + [p for p in accepted if problems(p)]
    source = 'val a = Regex("""x}""")\nval b = "a}".toRegex()\nval c = Regex("\\\\}")\nval d = Pattern.compile("$n}")\n'
    java = 'Pattern p = Pattern.compile("a}");\nboolean b = s.matches("\\\\}");\nString[] c = s.split("x}");\nString d = s.replace("}", "");\n'
    results = check([("t.kt", source), ("T.java", java)])
    if wrong or len(results) != 5:
        sys.exit(f"self-test failed: {wrong} {results}")


def main():
    self_test()
    failures = check(staged_sources() if "--staged" in sys.argv else tree_sources())
    if failures:
        print("Android's regex engine rejects these patterns:", file=sys.stderr)
        print("\n".join(failures), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
