import re
import unicodedata


def normalize(text):
    text = "".join(
        str(unicodedata.decimal(c)) if c.isdecimal() else c for c in unicodedata.normalize("NFKC", text)
    )
    return re.sub(r"[^a-z0-9]", "", text.lower())


def normalize_version(text):
    return normalize(re.sub(r"^v(?=\d)", "", text.strip(), flags=re.I))


def code_span(text, limit=80):
    flat = " ".join(text.replace("`", "'").split())
    return f"`{flat[:limit]}`"


FORM_HEADING = re.compile(r"###\s+(\S.*)")
BOLD_LABEL = re.compile(r"\*\*([^*:]+?):?\*\*:?(.*)")


def parse_fields(body, track_fences=True, heading=FORM_HEADING):
    fields = {}
    current = None
    fenced = False
    for line in re.split(r"\r\n|\r|\n", body):
        if track_fences and line.lstrip().startswith(("```", "~~~")):
            fenced = not fenced
        match = None if fenced else heading.match(line)
        name = match.group(1).strip() if match else None
        if name and name not in fields:
            current = name
            fields[current] = [match.group(2)] if match.re is BOLD_LABEL else []
        elif current:
            fields[current].append(line)
    if fenced:
        return parse_fields(body, track_fences=False, heading=heading)
    if not fields and heading is FORM_HEADING:
        return parse_fields(body, track_fences, heading=BOLD_LABEL)
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
