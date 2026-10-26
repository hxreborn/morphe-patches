#!/usr/bin/env python3
"""python3 triage.py <issue-body-file> [patches-list.json]"""

import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parents[2] / "scripts"))
from fields import code_span, field, has_any, normalize, normalize_version, parse_fields
from versions import prerelease_words, same_variant
from replies import APK_SOURCES, BROKEN_FORMS, bundle, code_list, missing

MIN_CHARS = 60

REPO = os.environ.get("GITHUB_REPOSITORY", "")
NEW_ISSUE = f"https://github.com/{REPO}/issues/new"

APP_NAME_FIELDS = ("App and version", "App name", "App", "Target app", "App version patched")
APP_VERSION_FIELDS = ("Other version", "App version", "Broken app version", "App version patched",
                      "App and version")
DESCRIPTION_FIELDS = ("Bug description", "What happens", "Describe the bug", "Summary")
STEP_FIELDS = ("Steps to reproduce", "Expected behaviour", "Actual behaviour")
LOG_FIELDS = ("Debug log", "Error logs", "Morphe logs", "Logs")
SOURCE_FIELDS = ("APK source", "APK source and type", "APK source and architecture")

REPACKAGERS = ("softonic", "happymod", "apkmod", "apkmody", "modyolo", "moddroid", "an1.com", "rexdl",
               "apkdone", "liteapks", "apkmb", "apk4free", "androeed", "apkrabi")

MARKER = "<!-- triage-bot -->"

REPORT = re.compile(
    r"Exception|Error|INSTALL_FAILED|Failed to match|Manager:|manager=|\(API \d+\)|^[ \t]+at \S"
    r"|Fatal signal|SIG(?:SEGV|ABRT|BUS|ILL)|backtrace:|user-attachments/files/"
    r"|https?://(?:www\.)?(?:pastebin\.com|gist\.github\.com|paste\.ee|hastebin\.com|dpaste\.org|rentry\.co)/\S+", re.I | re.M
)

OPTIONS_PATCHING_FAILED = ("patchingfailed", "patchingstartedandstoppedwithanerror")
OPTION_STOCK_FAILS_TOO = "theunpatchedappfailsthesameway"
OPTION_NOT_TRIED = "ihavenottried"
OPTIONS_PATCH_INEFFECTIVE = (
    "thepatchedfeatureisstilllocked",
    "thepatchhasnovisibleeffect",
    "apatchismissingfromthelistinmanager",
)

body_path = Path(sys.argv[1])
list_path = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("patches-list.json")


def supported_apps(data):
    apps = {}
    for patch in data["patches"]:
        for package in patch.get("compatiblePackages") or []:
            key = (package["name"], package["packageName"])
            versions = apps.setdefault(key, set())
            for target in package.get("targets") or []:
                if target.get("version"):
                    versions.add(target["version"])
    return apps


def host(url):
    try:
        return urlparse(url).hostname or ""
    except ValueError:
        return ""


def repackager_in(text):
    return next((h for h in REPACKAGERS if re.search(rf"(?:^|[/.@\s]){re.escape(h)}(?![a-z0-9])", text.lower())), "")


def version_cores(version):
    return {normalize(m.group(0)) for m in re.finditer(r"(?<![\d.])\d+(?:\.\d+)+", version)}


def matches_target(target):
    if prerelease_words(typed_version) != prerelease_words(target):
        return False
    if version_tokens & target_forms(target):
        return True
    cores = version_tokens | version_cores(app_version)
    return same_variant(typed_version, target) and bool(cores & (target_forms(target) | version_cores(target)))


data = json.loads(list_path.read_text(encoding="utf-8"))
apps = supported_apps(data)
fields = parse_fields(body_path.read_text(encoding="utf-8"))

description = field(fields, *DESCRIPTION_FIELDS) or "\n".join(filter(None, (field(fields, n) for n in STEP_FIELDS)))
if not has_any(fields, *DESCRIPTION_FIELDS, *STEP_FIELDS):
    description = max(fields.values(), key=len, default="")

app_name = field(fields, *APP_NAME_FIELDS)
reported_versions = [v for v in (field(fields, n) for n in APP_VERSION_FIELDS) if v]
app_version = " ".join(reported_versions)
typed_version = reported_versions[0] if reported_versions else ""
another_version = "another version" in app_name.lower() and not field(fields, "Other version")
shown_name = re.sub(r"\(another version, type it below\)", "", app_name, flags=re.I).strip()
reported = " ".join(dict.fromkeys(filter(None, (shown_name, typed_version))))
app_haystack = reported or "\n".join(fields.values())
version_words = [t for t in re.split(r"[\s(),`]+", app_version) if t.strip()]
version_tokens = {normalize_version(t) for t in version_words} | {
    normalize(a) + normalize(b) for a, b in zip(version_words, version_words[1:])
}
version_haystack = normalize(app_haystack)
what_happened = field(fields, "What happened")
apk_source = field(fields, *SOURCE_FIELDS)
source_hosts = [host(url) for url in re.findall(r"https?://\S+", apk_source, re.I)]
repackager_host = repackager_in(" ".join(source_hosts)) if all(map(repackager_in, source_hosts)) else ""
repackager_named = repackager_in(apk_source)
debug_log = field(fields, *LOG_FIELDS)
report_attached = bool(REPORT.search(debug_log))


def target_forms(version):
    numbered = [w for w in version.split() if any(c.isdigit() for c in w)]
    return {normalize_version(version)} | {normalize_version(w) for w in numbered}


blockers = []
flags = []
notes = []
labels = []

if not fields:
    notes.append(
        f"This issue has no form fields, so it can't be checked. Please open it again from one of the forms:\n\n{BROKEN_FORMS}"
    )
    labels.append("needs info")

if fields and not report_attached and len(description) < MIN_CHARS:
    flags.append(
        f"The description has {len(description)} characters. It needs at least {MIN_CHARS}."
    )
    labels.append("needs info")

matched = None
best = 0
normalized_app = normalize(app_haystack)
for (name, package), versions in apps.items():
    signals = [normalize(name), normalize(package)]
    signals += [normalize(word) for word in name.split() if len(word) > 3]
    score = max((len(sig) for sig in signals if sig and sig in normalized_app), default=0)
    if score > best:
        best, matched = score, (name, versions)

if repackager_host:
    blockers.append(
        f"`{repackager_host}` repackages apps, which can break patching. "
        f"Download the original APK from {APK_SOURCES} and patch it again."
    )
elif repackager_named:
    flags.append(
        f"The APK source mentions `{repackager_named}`, which repackages apps and can break patching. "
        f"Please download the original APK from {APK_SOURCES} and patch it again."
    )
    labels.append("needs info")


if not fields:
    pass
elif not matched:
    supported = ", ".join(sorted(name for name, _ in apps))
    lead = (
        f"{code_span(reported)} isn't supported by this bundle."
        if reported
        else "The report doesn't name a supported app."
    )
    flags.append(
        f"{lead} {bundle(data['version'])} supports {supported}."
    )
    labels.append("needs info")
elif another_version:
    flags.append(
        "\"Another version\" is selected, but the Other version field is empty."
    )
    labels.append("needs info")
elif matched[1] and not (
    any(matches_target(v) for v in matched[1]) if version_tokens
    else any(form in version_haystack for v in matched[1] for form in target_forms(v))
):
    name, versions = matched
    typed = code_span(typed_version or reported)
    notes.append(
        f"{bundle(data['version'])} supports {name} {code_list(versions)}, but not {typed} yet. Please either:\n\n"
        f"- patch one of the supported versions\n- open an [app update request]({NEW_ISSUE}?template=app_update.yml) for {typed}"
    )
    labels.append("untargeted version")

if normalize(what_happened).startswith(OPTIONS_PATCHING_FAILED) and not report_attached:
    flags.append(
        "Patching failed, but the Error logs field has no error report. "
        "Please tap Copy on Manager's error dialog and paste it there."
    )
    labels.append("needs info")

failure = normalize(field(fields, "What does not work"))
if failure and not report_attached:
    flags.append(
        "The Error logs field has no log. Please attach the `adb logcat` file or the LogFox export."
    )
    labels.append("needs info")

patch_ineffective = failure.startswith(OPTIONS_PATCH_INEFFECTIVE)
stock = "" if patch_ineffective else normalize(field(fields, "Does the unpatched app do the same thing"))

if stock.startswith(OPTION_STOCK_FAILS_TOO):
    flags.append(
        "The unpatched app has the same problem. Please say whether the patched app behaves any differently."
    )
    labels.append("needs info")
elif stock.startswith(OPTION_NOT_TRIED):
    flags.append(
        "The unpatched app hasn't been tried yet. Please install it and say whether it does the same."
    )
    labels.append("needs info")

if blockers:
    verdict = "close"
    labels = []
    sections = ["This issue is now closed:\n\n" + "\n".join(f"- {line}" for line in blockers + flags)]
elif flags or notes:
    verdict = "flag"
    sections = notes + ([missing(flags)] if flags else [])
else:
    verdict = "pass"
    sections = []

print(json.dumps({
    "verdict": verdict,
    "labels": list(dict.fromkeys(labels)),
    "app": matched[0] if matched else "",
    "comment": MARKER + "\n" + "\n\n".join(sections) if sections else "",
}))
