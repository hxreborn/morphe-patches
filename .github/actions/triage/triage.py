#!/usr/bin/env python3
"""python3 triage.py <issue-body-file> [patches-list.json]"""

import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from fields import CONTEST, field, has_any, normalize, normalize_version, parse_fields

MIN_CHARS = 60

REPO = os.environ.get("GITHUB_REPOSITORY", "")
NEW_ISSUE = f"https://github.com/{REPO}/issues/new/choose" if REPO else ""

APP_NAME_FIELDS = ("App and version", "App name", "App", "Target app", "App version patched")
APP_VERSION_FIELDS = ("Other version", "App version", "Broken app version", "App version patched",
                      "App and version")
DESCRIPTION_FIELDS = ("Bug description", "What happens", "Describe the bug", "Summary")
LOG_FIELDS = ("Debug log", "Error logs", "Morphe logs", "Logs")
SOURCE_FIELDS = ("APK source", "APK source and type", "APK source and architecture")

REPACKAGERS = ("softonic", "happymod", "apkmody", "modyolo", "moddroid", "an1.com", "rexdl",
               "apkdone", "liteapks", "apkmb", "apk4free", "androeed", "apkrabi")

MARKER = "<!-- triage-bot -->"
NO_RESPONSE = "_no response_"

REPORT = re.compile(
    r"Exception|Error|INSTALL_FAILED|Failed to match|Manager:|manager=|\(API \d+\)|^\s+at \S", re.I | re.M
)

OPTION_PATCHING_FAILED = "patchingfailed"
OPTION_STOCK_FAILS_TOO = "theunpatchedappfailsthesameway"
OPTION_NOT_TRIED = "ihavenottried"

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


data = json.loads(list_path.read_text(encoding="utf-8"))
apps = supported_apps(data)
fields = parse_fields(body_path.read_text(encoding="utf-8"))

description = field(fields, *DESCRIPTION_FIELDS)
if not has_any(fields, *DESCRIPTION_FIELDS):
    description = max(fields.values(), key=len, default="")

app_name = field(fields, *APP_NAME_FIELDS)
app_version = " ".join(filter(None, (field(fields, n) for n in APP_VERSION_FIELDS)))
another_version = "another version" in app_name.lower() and not field(fields, "Other version")
reported = " ".join(dict.fromkeys(filter(None, (app_name, app_version))))
app_haystack = reported or "\n".join(fields.values())
version_words = [t for t in re.split(r"[\s(),`]+", app_version) if t.strip()]
version_tokens = {normalize_version(t) for t in version_words} | {
    normalize(a) + normalize(b) for a, b in zip(version_words, version_words[1:])
}
version_haystack = normalize(app_haystack)
what_happened = field(fields, "What happened")
apk_source = field(fields, *SOURCE_FIELDS)
repackager = next(
    (h for h in REPACKAGERS if re.search(rf"(?:^|[/.@\s]){re.escape(h)}(?![a-z0-9])", apk_source.lower())), ""
)
debug_log = field(fields, *LOG_FIELDS)
report_attached = bool(REPORT.search(debug_log))


def target_forms(version):
    numbered = [w for w in version.split() if any(c.isdigit() for c in w)]
    return {normalize_version(version)} | {normalize_version(w) for w in numbered}


blockers = []
flags = []
labels = []

if not fields:
    blockers.append(
        "**No issue form.** The triage reads the form fields. "
        + (f"Open [a new issue]({NEW_ISSUE}) and pick the matching form." if NEW_ISSUE else "Open a new issue and pick the matching form.")
    )

if fields and not report_attached and len(description) < MIN_CHARS:
    flags.append(
        f"**The description is short** ({len(description)} characters, {MIN_CHARS} needed). Add "
        "what you did, what you expected and what happened instead."
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

if repackager:
    blockers.append(
        f"**`{repackager}` repackages apps**, so patches may not apply cleanly. Patch an "
        "unmodified APK from APKMirror, APKPure, Uptodown or APKCombo instead."
    )

if not fields:
    pass
elif not matched:
    supported = ", ".join(sorted(name for name, _ in apps))
    lead = (
        f"**`{reported}` isn't patched by this bundle yet.**"
        if reported
        else "**The report doesn't name an app this bundle patches.**"
    )
    flags.append(
        f"{lead} Supported apps in {data['version']}: {supported}. "
        "To get another app added, open an app request."
    )
    labels.append("needs info")
elif another_version:
    flags.append(
        "**\"Another version\" is picked but the version is missing.** Fill in the Other version "
        "field. The number is on the app's About screen."
    )
    labels.append("needs info")
elif matched[1] and not (
    any(form in version_tokens for v in matched[1] for form in target_forms(v)) if version_tokens
    else any(form in version_haystack for v in matched[1] for form in target_forms(v))
):
    name, versions = matched
    flags.append(
        f"**{data['version']} targets {name} {', '.join(sorted(versions))}**, not "
        f"`{app_version or reported}`. Either the app updated or it was patched with `-f`. Say which."
    )
    labels.append("untargeted version")

if normalize(what_happened).startswith(OPTION_PATCHING_FAILED) and not report_attached:
    flags.append(
        "**Patching failed but no error report is attached.** In Morphe Manager, tap **Copy** on the "
        "error dialog and paste it here. With morphe-cli, add `-r report.json` and attach the "
        "report."
    )
    labels.append("needs info")

stock = normalize(field(fields, "Does the unpatched app do the same thing"))
single_patch = normalize(field(fields, "Does it still happen with only one patch selected"))

if stock.startswith(OPTION_STOCK_FAILS_TOO):
    flags.append(
        "**The unpatched app fails the same way**, so the app is the likely cause rather than a "
        "patch. If the patched build behaves differently, change that answer and describe the difference."
    )
    labels.append("needs info")
elif stock.startswith(OPTION_NOT_TRIED):
    flags.append(
        "**The unpatched app hasn't been tried.** Install the stock APK, repeat the same steps and "
        "add the result to the report."
    )
    labels.append("needs info")

if single_patch.startswith(OPTION_NOT_TRIED):
    flags.append(
        "**Narrow it down to one patch.** Patch again with only the reported patch selected. If the "
        "problem stops, enable the others one at a time until it returns, then pick the culprit in "
        "the form."
    )
    if "needs info" not in labels:
        labels.append("needs info")

if blockers:
    verdict = "close"
    labels = []
    lines = ["🚧 **Closing this for now.**", ""]
    lines += [f"- {b}" for b in blockers + flags]
    lines += ["", "Edit the issue to cover the points above and it reopens automatically."]
elif flags:
    verdict = "flag"
    lines = ["📝 **A few details are missing.**", ""]
    lines += [f"- {f}" for f in flags]
    lines += ["", "Edit the issue to add them."]
else:
    verdict = "pass"
    lines = []

if lines:
    lines += ["", CONTEST]

print(json.dumps({
    "verdict": verdict,
    "labels": list(dict.fromkeys(labels)),
    "app": matched[0] if matched else "",
    "comment": MARKER + "\n" + "\n".join(lines) if lines else "",
}))
