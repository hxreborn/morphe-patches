import re


def normalize(text):
    return re.sub(r"[^a-z0-9]", "", text.lower())


def normalize_version(text):
    return normalize(re.sub(r"^v(?=\d)", "", text.strip(), flags=re.I))


def parse_fields(body):
    fields = {}
    current = None
    fenced = False
    for line in body.splitlines():
        if line.lstrip().startswith("```"):
            fenced = not fenced
        heading = None if fenced else re.match(r"^#{2,3}\s+(.+?)\s*$", line)
        if heading:
            current = heading.group(1)
            fields[current] = []
        elif current:
            fields[current].append(line)
    return {k: "\n".join(v).strip() for k, v in fields.items()}


def has_any(fields, *names):
    wanted = {normalize(n) for n in names}
    return any(normalize(key) in wanted for key in fields)


def field(fields, *names):
    for name in names:
        for key, value in fields.items():
            if normalize(key) == normalize(name):
                return "" if value.lower() == "_no response_" else value
    return ""
