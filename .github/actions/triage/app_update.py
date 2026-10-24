#!/usr/bin/env python3
"""python3 app_update.py <issue-number> <issue-body-file> <updates.json> <patches-list.json>"""

import json
import re
import sys
import unicodedata
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parents[2] / "scripts"))
from fields import field, normalize, parse_fields
from versions import (APP_UPDATE_TITLE_PREFIX, build_tags, compare, same_variant, targets, tracked_version,
                      version_key)
from replies import BROKEN_FORMS, bundle

MARKER = "<!-- app-update-bot -->"
VERSION_CODE = re.compile(r"\s*\(([\w.+-]*)\)\s*$")
VERSION = re.compile(r"[A-Za-z0-9._+-]+(?: [A-Za-z0-9._+-]+)?")
MAX_VERSION = 64


def is_version(text):
    return bool(VERSION.fullmatch(text)) and sum(bool(re.search(r"\d\.\d", word)) for word in text.split()) == 1


def requested_version(body):
    raw = unicodedata.normalize("NFKC", field(parse_fields(body), "New version", "Version")).strip().strip("`")
    if len(raw) > 2 * MAX_VERSION:
        return ""
    raw = VERSION_CODE.sub(lambda m: f" {m.group(1)}" if build_tags(m.group(1)) else "", raw)
    version = re.sub(r"^v(?=\d)", "", raw, flags=re.I).strip()
    return version if len(version) <= MAX_VERSION and is_version(version) else ""


def requested_app(body, apps):
    name = normalize(field(parse_fields(body), "App"))
    return next((app for app in apps if name and normalize(app) == name), "")


def open_tracker(app, version, number, issues, apps):
    tracked = [(i, tracked_version(i["title"], app, apps)) for i in issues
               if i["number"] != number and i["state"] == "OPEN"
               and not {"needs info", "bot-wrong"} & {label["name"] for label in i.get("labels") or []}]
    return max(((i, v) for i, v in tracked if v and is_version(v) and build_tags(v) == build_tags(version)),
               key=lambda iv: version_key(iv[1]), default=(None, ""))


def decide(number, body, issues, patches_list):
    apps = targets(patches_list)
    app = requested_app(body, apps)
    version = requested_version(body)
    verdict, reason, duplicate_of, labels, title, retitle, lines = "pass", "", 0, [], "", None, []

    if not app:
        verdict, labels = "flag", ["needs info"]
        lines.append("The App field doesn't match an app this bundle supports. Please edit the issue and pick it from the list.")
    elif not re.search(r"\d", version):
        verdict, labels = "flag", ["needs info"]
        example = max(apps[app], key=version_key, default="1.2.3")
        lines.append(f"The New version field needs a single version number, like `{example}`. Please edit the issue to fix it.")
    else:
        current = max((t for t in apps[app] if same_variant(version, t)), key=version_key, default="")
        tracker, tracked = open_tracker(app, version, number, issues, apps)
        if any(compare(version, target) == 0 and same_variant(version, target) for target in apps[app]):
            verdict, reason = "close", "completed"
            lines.append(f"{bundle(patches_list['version'])} already supports {app} `{version}`. This issue is now closed. "
                         f"If it doesn't patch or work, open an issue:\n\n{BROKEN_FORMS}")
        elif current and compare(version, current) < 0:
            verdict, reason = "close", "completed"
            lines.append(f"{bundle(patches_list['version'])} already supports {app} `{current}`, which is newer than `{version}`. "
                         f"This issue is now closed. Please update the app to `{current}` and repatch.")
        elif tracker:
            verdict, reason, duplicate_of = "close", "duplicate", tracker["number"]
            if compare(version, tracked) > 0:
                retitle = {"number": tracker["number"], "title": f"{APP_UPDATE_TITLE_PREFIX}{app} {version}"}
                lines.append(f"Thanks! #{tracker['number']} now tracks {app} `{version}`, so this issue is now closed as a duplicate. "
                             f"Please follow #{tracker['number']} for updates.")
            else:
                lines.append(f"#{tracker['number']} already tracks {app} `{tracked}`, so this issue is now closed as a duplicate. "
                             f"Please follow #{tracker['number']} for updates.")
        else:
            title = f"{APP_UPDATE_TITLE_PREFIX}{app} {version}"

    return {
        "verdict": verdict,
        "reason": reason,
        "duplicate_of": duplicate_of,
        "labels": labels,
        "title": title,
        "retitle": retitle,
        "comment": MARKER + "\n" + "\n".join(lines) if lines else "",
    }


if __name__ == "__main__":
    number = int(sys.argv[1])
    body = Path(sys.argv[2]).read_text(encoding="utf-8")
    issues = json.loads(Path(sys.argv[3]).read_text(encoding="utf-8"))
    patches_list = json.loads(Path(sys.argv[4]).read_text(encoding="utf-8"))
    print(json.dumps(decide(number, body, issues, patches_list)))
