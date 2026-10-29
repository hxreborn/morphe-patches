#!/usr/bin/env python3
"""python3 .github/actions/triage/test_feature_request.py"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from feature_request import decide

PATCHES = {
    "version": "1.66.0",
    "patches": [{"compatiblePackages": [
        {"name": "Proton Mail", "packageName": "ch.protonmail.android", "targets": [{"version": "4.15.0"}]},
        {"name": "MovieBox", "packageName": "com.community.oneroom", "targets": [{"version": "4.0.03.1020.03"}]},
        {"name": "BeReal", "packageName": "com.bereal.ft", "targets": []},
        {"name": "imo", "packageName": "com.imo.android.imoim", "targets": []},
    ]}],
}


def body(app):
    return f"### App\n\n{app}\n\n### Feature description\n\nUnlock everything\n"


CASES = {
    "an unsupported app is closed": (body("Titans Fam"), "close"),
    "a supported app passes": (body("Proton Mail"), "pass"),
    "a supported app with a version passes": (body("MovieBox 4.0.03"), "pass"),
    "a supported package name passes": (body("ch.protonmail.android"), "pass"),
    "one word of a supported app's name passes": (body("proton"), "pass"),
    "a differently spaced supported name passes": (body("Be Real"), "pass"),
    "an empty App field passes": (body("_No response_"), "pass"),
    "a body without the App field passes": ("### Feature description\n\nx\n", "pass"),
    "a one-word unknown app is closed": (body("Titans"), "close"),
    "a short supported name passes": (body("imo"), "pass"),
    "a word too short to judge passes": (body("all"), "pass"),
    "a version without an app name passes": (body("4.0.03.0918.03"), "pass"),
}

failures = 0
for name, (issue_body, expected) in CASES.items():
    actual = decide(issue_body, PATCHES)["verdict"]
    ok = actual == expected
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected}, got {actual}")

comment = decide(body("Titans Fam"), PATCHES)["comment"]
ok = (comment.startswith("<!-- feature-request-bot -->\n`Titans Fam` isn't supported by this bundle")
      and "discussions/new?category=app-requests" in comment)
failures += not ok
print(f"{'ok  ' if ok else 'FAIL'} the close comment names the app and links app requests")

total = len(CASES) + 1
print(f"\n{total - failures}/{total} passed")
sys.exit(1 if failures else 0)
