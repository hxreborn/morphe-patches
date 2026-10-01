import re

CONTEST = "comment if this is wrong"


def normalize(text):
    return re.sub(r"[^a-z0-9]", "", text.lower())


def normalize_version(text):
    return normalize(re.sub(r"^v(?=\d)", "", text.strip(), flags=re.I))


def parse_fields(body, track_fences=True):
    fields = {}
    current = None
    fenced = False
    for line in body.splitlines():
        if track_fences and line.lstrip().startswith(("```", "~~~")):
            fenced = not fenced
        heading = None if fenced else re.match(r"^###\s+(.+?)\s*$", line)
        if heading and heading.group(1) not in fields:
            current = heading.group(1)
            fields[current] = []
        elif current:
            fields[current].append(line)
    if fenced:
        return parse_fields(body, track_fences=False)
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
