#!/usr/bin/env python3
"""python3 triage.py <issue-body-file> [patches-list.json]"""

import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from fields import field, has_any, normalize, normalize_version, parse_fields

MIN_CHARS = 60
MIN_WORDS = 10

REPO = os.environ.get("GITHUB_REPOSITORY", "")
NEW_ISSUE = f"https://github.com/{REPO}/issues/new/choose" if REPO else ""

APP_NAME_FIELDS = ("App and version", "App name", "App", "Target app", "App version patched")
APP_VERSION_FIELDS = ("Other version", "App version", "Broken app version", "App version patched",
                      "App and version")
DESCRIPTION_FIELDS = ("Bug description", "What happens", "What happened?", "Describe the bug",
                      "Summary")
LOG_FIELDS = ("Debug log", "Error logs", "Morphe logs", "Logs")
SOURCE_FIELDS = ("APK source", "APK source and type", "APK source and architecture")

REPACKAGERS = ("softonic", "happymod", "apkmody", "modyolo", "moddroid", "an1.com", "rexdl",
               "apkdone", "liteapks", "apkmb", "apk4free", "androeed", "apkrabi")

MARKER = "<!-- triage-bot -->"
NO_RESPONSE = "_no response_"

REPORT_MARKERS = ("PatchException", "Manager:", "manager=", "Failed to match")

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
reported = " ".join(filter(None, (app_name, app_version)))
app_haystack = reported or "\n".join(fields.values())
version_tokens = {normalize_version(t) for t in re.split(r"[\s(),`]+", app_version) if t.strip()}
version_haystack = normalize(app_haystack)
what_happened = field(fields, "What happened")
apk_source = field(fields, *SOURCE_FIELDS)
source_text = (apk_source + "\n" + "\n".join(fields.values())).lower()
repackager = next((h for h in REPACKAGERS if h in source_text), "")
debug_log = field(fields, *LOG_FIELDS)

blockers = []
flags = []
labels = []

if not fields:
    blockers.append(
        "**This wasn't filed through an issue form.** "
        + (f"Open [a new issue]({NEW_ISSUE}) and pick one." if NEW_ISSUE else "Open one and pick a form.")
    )

words = len(description.split())
if fields and (len(description) < MIN_CHARS or words < MIN_WORDS):
    blockers.append(
        f"**The description is too short** ({words} words, minimum {MIN_WORDS}). Say what you "
        "did, what you expected, and what happened instead."
    )

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
        f"**The APK came from `{repackager}`, which repackages apps.** Get the stock APK from "
        "APKMirror, APKPure, Uptodown or APKCombo and patch that."
    )

if not fields:
    pass
elif not matched:
    supported = ", ".join(sorted(name for name, _ in apps))
    lead = (
        f"**`{reported}` isn't an app this bundle patches.**"
        if reported
        else "**The report doesn't name an app this bundle patches.**"
    )
    blockers.append(
        f"{lead} Supported apps in {data['version']}: {supported}. "
        "Open an app request instead if you want it added."
    )
elif another_version:
    flags.append(
        "**You picked \"another version\" but didn't say which.** Put the version from the app's "
        "About screen in the Other version field."
    )
    labels.append("needs info")
elif matched[1] and not (
    any(normalize_version(v) in version_tokens for v in matched[1]) if version_tokens
    else any(normalize_version(v) in version_haystack for v in matched[1])
):
    name, versions = matched
    flags.append(
        f"**{data['version']} targets {name} {', '.join(sorted(versions))}**, not "
        f"`{app_version or reported}`. Either the app updated and needs retargeting, or you "
        "patched with `-f`. Say which."
    )
    labels.append("untargeted version")

if normalize(what_happened).startswith(OPTION_PATCHING_FAILED) and not any(
    marker in debug_log for marker in REPORT_MARKERS
):
    flags.append(
        "**Patching failed, so I need the error report.** Tap **Copy** on Morphe Manager's "
        "error dialog and paste it here. With morphe-cli, add `-r report.json` and attach the "
        "report."
    )
    labels.append("needs info")

stock = normalize(field(fields, "Does the unpatched app do the same thing"))
single_patch = normalize(field(fields, "Does it still happen with only one patch selected"))

if stock.startswith(OPTION_STOCK_FAILS_TOO):
    blockers.append(
        "**You said the unpatched app fails the same way.** Reopen if the patched build "
        "behaves differently."
    )
elif stock.startswith(OPTION_NOT_TRIED):
    flags.append(
        "**Try the unpatched app first.** Install the stock APK, repeat the steps, and say what "
        "happened."
    )
    labels.append("needs info")

if single_patch.startswith(OPTION_NOT_TRIED):
    flags.append(
        "**Narrow it down to one patch.** Patch again with only that one patch selected. If that "
        "build works, turn the others back on one at a time until it breaks, and pick the one "
        "that did it in the form."
    )
    if "needs info" not in labels:
        labels.append("needs info")

if blockers:
    verdict = "close"
    labels = []
    lines = ["Closing this for now:", ""]
    lines += [f"- {b}" for b in blockers + flags]
    lines += ["", "**Edit the issue to fix this and it reopens automatically.**"]
elif flags:
    verdict = "flag"
    lines = ["This report is missing something:", ""]
    lines += [f"- {f}" for f in flags]
    lines += ["", "Edit the issue to add it."]
else:
    verdict = "pass"
    lines = []

if lines:
    lines += ["", "If this is wrong, add the `bad-triage` label or say so below."]

print(json.dumps({
    "verdict": verdict,
    "labels": labels,
    "app": matched[0] if matched else "",
    "comment": MARKER + "\n" + "\n".join(lines) if lines else "",
}))
