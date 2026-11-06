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


PLACEHOLDERS = {"na", "no", "none", "null", "nil", "nothing", "idk", "same", "seetitle", "seeabove", "asdf", "test",
                "ok", "lol", "noidea", "dunno", "unknown", "notsure", "nope", "nah", "yes", "abc", "qwerty"}

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


MIN_ANSWER_LETTERS = 8
MIN_ANSWER_WORDS = 2
IMAGE = re.compile(r"user-attachments/|!\[|<img\b|https?://\S+", re.I)
FEATURE_ASK = re.compile(
    r"\b(?:please|pls|plz|can you|could you|would you|kindly)\s+(?:also\s+)?add\b|\bwould be (?:nice|great|cool|awesome)\b"
    r"|\bfeature request\b|\badd (?:an? )?(?:option|feature|patch|toggle|setting)\b|\bi(?: would)? (?:want|like|wish)\b",
    re.I,
)


def is_placeholder(value):
    letters = re.sub(r"[\W_]+", "", unicodedata.normalize("NFKC", value).casefold())
    return letters in PLACEHOLDERS or letters.isdigit() or len(letters) < 2


def is_thin(value):
    text = unicodedata.normalize("NFKC", value)
    words = {w.casefold() for w in re.findall(r"[^\W\d_]{2,}", text)}
    spaced = len(text.split()) > 1
    return sum(c.isalpha() for c in text) < MIN_ANSWER_LETTERS or (spaced and len(words) < MIN_ANSWER_WORDS)


def placeholder_answers(fields, *names):
    return [f"The {name} field says {code_span(value)}, which isn't an answer."
            for name in names if (value := field(fields, name)) and is_placeholder(value)]


def thin_answers(fields, *names):
    return [f"The {name} field says {code_span(value)}, which doesn't describe anything."
            for name in names if (value := field(fields, name)) and not is_placeholder(value) and is_thin(value)]


def has_image(value):
    return bool(IMAGE.search(value))


def asks_for_feature(text):
    return bool(FEATURE_ASK.search(text))


def words(text):
    return re.findall(r"[a-z0-9]+", unicodedata.normalize("NFKC", text).lower())


def joined(text):
    return "".join(words(text))


def mentions(text, name, package):
    spaced = f" {' '.join(words(text))} "
    return (f" {' '.join(words(name))} " in spaced or f" {joined(name)} " in spaced
            or bool(re.search(rf"(?<![\w.]){re.escape(package)}(?![\w.])", text, re.I)))


def nested(name, other):
    return joined(name) in joined(other) or joined(other) in joined(name)


def contradicting_app(chosen, text, apps):
    if not chosen or any(mentions(text, name, package) for name, package in apps if name == chosen):
        return ""
    named = {name for name, package in apps if mentions(text, name, package) and not nested(name, chosen)}
    longest = {name for name in named if not any(name != other and joined(name) in joined(other) for other in named)}
    return longest.pop() if len(longest) == 1 else ""


def app_packages(patches_list):
    return {(package["name"], package["packageName"])
            for patch in patches_list["patches"] for package in patch.get("compatiblePackages") or []}
