#!/usr/bin/env python3
"""python3 .github/scripts/test_shipped_requests.py"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from shipped_requests import prerelease_comment, shipped


def patches_list(*apps):
    return {"patches": [{"compatiblePackages": [{"name": name, "packageName": package}]} for name, package in apps]}


def request(number, app, package, state="OPEN", features="Unlock premium"):
    body = f"### App\n\n{app}\n\n### Package name\n\n{package}\n\n### Requested features\n\n{features}\n"
    return {"number": number, "state": state, "body": body}


PREVIOUS = patches_list(("Old App", "com.old.app"))
CURRENT = patches_list(("Old App", "com.old.app"), ("New App", "com.new.app"), ("Other App", "com.other.app"))
REQUESTS = [
    request(1, "New App", "com.new.app"),
    request(2, "New App again", "COM.New.App", features="Unlock premium, Remove ads"),
    request(3, "Old App", "com.old.app"),
    request(4, "Closed", "com.other.app", state="CLOSED"),
    request(5, "Linked, https://play.google.com/store/apps/details?id=com.other.app", "_No response_",
            features="Something else, described below"),
    request(6, "Unrelated", "com.unrelated.app"),
    request(7, "New App once more", "com.new.app", features="_No response_"),
]

failures = 0
checks = 0


def check(ok, name):
    global failures, checks
    checks += 1
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}")


rows = {row["number"]: row for row in shipped(PREVIOUS, CURRENT, REQUESTS)}
check(sorted(rows) == [1, 2, 5, 7], "open requests naming a package new in this release are listed")
check(all(row["app"] == ("New App" if row["package"] == "com.new.app" else "Other App") for row in rows.values()),
      "each row carries the app name from the patches list")
check(shipped(CURRENT, CURRENT, REQUESTS) == [], "a release that adds no package lists nothing")

comment = prerelease_comment(rows[1], "v1.2.0-dev.3", "owner/repo")
check(comment == "<!-- prerelease-bot -->\nNew App has been added in the pre-release "
      "[v1.2.0-dev.3](https://github.com/owner/repo/releases/tag/v1.2.0-dev.3). Please try it and reply whether "
      "it works. This discussion closes when it reaches a stable release.\n",
      "a shipped request gets the pre-release line and the close notice")

print(f"\n{checks - failures}/{checks} passed")
sys.exit(1 if failures else 0)
