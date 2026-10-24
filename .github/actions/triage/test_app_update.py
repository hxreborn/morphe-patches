#!/usr/bin/env python3
"""python3 .github/actions/triage/test_app_update.py"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from app_update import decide

PATCHES = {
    "version": "1.49.1",
    "patches": [{"compatiblePackages": [
        {"name": "Proton VPN", "packageName": "ch.protonvpn.android",
         "targets": [{"version": "5.20.39.0"}, {"version": "5.20.57.0"}]},
        {"name": "Showly", "packageName": "com.michaldrabik.showly2", "targets": [{"version": "3.72.0"}]},
        {"name": "Cxxdroid", "packageName": "ru.iiec.cxxdroid", "targets": [{"version": "6.0_arm64"}]},
        {"name": "MovieBox", "packageName": "com.community.oneroom", "targets": [{"version": "4.0.03.0918.03"}]},
        {"name": "MovieBox TV", "packageName": "com.community.mbox.tv", "targets": [{"version": "1.2.0.0923"}]},
        {"name": "Demo", "packageName": "com.demo", "targets": [{"version": "2.0 beta"}]},
    ]}],
}


def body(app, version):
    return f"### App\n\n{app}\n\n### New version\n\n{version}\n\n### APK source\n\nhttps://apkmirror.com/x\n"


def tracker(number, app, version, state="OPEN"):
    return {"number": number, "title": f"[App Update]: {app} {version}", "state": state}


CASES = {
    "newer version with no tracker gets the canonical title": (
        body("Proton VPN", "5.20.60.0"), [], ("pass", "", 0, "[App Update]: Proton VPN 5.20.60.0", None)),
    "version code in parentheses is dropped": (
        body("Proton VPN", "v5.20.60.0 (605206000)"), [], ("pass", "", 0, "[App Update]: Proton VPN 5.20.60.0", None)),
    "version already targeted closes as completed": (
        body("Proton VPN", "5.20.57.0"), [], ("close", "completed", 0, "", None)),
    "older version closes as completed": (
        body("Proton VPN", "5.20.21.0"), [], ("close", "completed", 0, "", None)),
    "same version as the open tracker closes as its duplicate": (
        body("Showly", "3.73.0"), [tracker(7, "Showly", "3.73.0")], ("close", "duplicate", 7, "", None)),
    "older than the open tracker closes as its duplicate": (
        body("Showly", "3.72.5"), [tracker(7, "Showly", "3.73.0")], ("close", "duplicate", 7, "", None)),
    "newer than the open tracker retitles it": (
        body("Showly", "3.74.0"), [tracker(7, "Showly", "3.73.0")],
        ("close", "duplicate", 7, "", {"number": 7, "title": "[App Update]: Showly 3.74.0"})),
    "closed tracker for the same version is not a duplicate target": (
        body("Showly", "3.73.0"), [tracker(7, "Showly", "3.73.0", "CLOSED")],
        ("pass", "", 0, "[App Update]: Showly 3.73.0", None)),
    "another app's tracker is ignored": (
        body("Showly", "3.73.0"), [tracker(7, "Proton VPN", "5.20.60.0")],
        ("pass", "", 0, "[App Update]: Showly 3.73.0", None)),
    "the issue itself is not its own tracker": (
        body("Showly", "3.73.0"), [tracker(9, "Showly", "3.73.0")], ("pass", "", 0, "[App Update]: Showly 3.73.0", None)),
    "unknown app asks for info": (
        body("Nope", "1.0"), [], ("flag", "", 0, "", None)),
    "missing version asks for info": (
        body("Showly", "_No response_"), [], ("flag", "", 0, "", None)),
    "markup after the version asks for info instead of retitling the tracker": (
        body("Showly", "99 @octocat [Download](https://example.com/x.apk)"), [tracker(7, "Showly", "3.73.0")],
        ("flag", "", 0, "", None)),
    "a longer app's tracker is not this app's tracker": (
        body("MovieBox", "4.0.04.0930.03"), [tracker(7, "MovieBox TV", "1.2.0.0924")],
        ("pass", "", 0, "[App Update]: MovieBox 4.0.04.0930.03", None)),
    "a tracker whose title holds no version is ignored": (
        body("Showly", "3.73.0"), [{"number": 7, "title": "[App Update]: Showly 3.80 @octocat [x](y)", "state": "OPEN"}],
        ("pass", "", 0, "[App Update]: Showly 3.73.0", None)),
    "a version without the build tag matches the tagged target": (
        body("Cxxdroid", "6.0"), [], ("close", "completed", 0, "", None)),
    "a device ABI on a universal target is already supported": (
        body("Proton VPN", "5.20.57.0 arm64"), [], ("close", "completed", 0, "", None)),
    "a parenthesised ABI on a universal target is already supported": (
        body("Proton VPN", "5.20.57.0 (arm64-v8a)"), [], ("close", "completed", 0, "", None)),
    "a TV build of a targeted phone version is a new request": (
        body("Showly", "3.72.0 TV"), [], ("pass", "", 0, "[App Update]: Showly 3.72.0 TV", None)),
    "a bare version code asks for info": (
        body("Showly", "11782"), [tracker(7, "Showly", "3.73.0")], ("flag", "", 0, "", None)),
    "a flagged issue is not the tracker": (
        body("Showly", "3.74.0"), [{**tracker(7, "Showly", "3.73.0"), "labels": [{"name": "needs info"}]}],
        ("pass", "", 0, "[App Update]: Showly 3.74.0", None)),
    "a bot-wrong tracker is left alone": (
        body("Showly", "3.74.0"), [{**tracker(7, "Showly", "3.73.0"), "labels": [{"name": "bot-wrong"}]}],
        ("pass", "", 0, "[App Update]: Showly 3.74.0", None)),
    "a title naming the app without a version is not a tracker": (
        body("MovieBox", "4.0.04.0930.03"), [{"number": 7, "title": "[App Update]: MovieBox TV", "state": "OPEN"}],
        ("pass", "", 0, "[App Update]: MovieBox 4.0.04.0930.03", None)),
    "fullwidth digits are read as the version": (
        body("Showly", "\uff13.\uff17\uff13.\uff10"), [], ("pass", "", 0, "[App Update]: Showly 3.73.0", None)),
    "an older version of an untargeted ABI is not closed as older": (
        body("Cxxdroid", "5.7_armv7"), [], ("pass", "", 0, "[App Update]: Cxxdroid 5.7_armv7", None)),
    "a tracker for another ABI is not this version's tracker": (
        body("Cxxdroid", "6.1_arm64"), [tracker(7, "Cxxdroid", "6.0.5_x86_64")],
        ("pass", "", 0, "[App Update]: Cxxdroid 6.1_arm64", None)),
    "the release of a prerelease target is a new request": (
        body("Demo", "2.0"), [], ("pass", "", 0, "[App Update]: Demo 2.0", None)),
    "a parenthesised ABI of an untargeted build is a new request": (
        body("Cxxdroid", "6.0 (x86_64)"), [], ("pass", "", 0, "[App Update]: Cxxdroid 6.0 x86_64", None)),
    "two version numbers ask for info": (
        body("Showly", "3.74.0 3.75.0"), [tracker(7, "Showly", "3.73.0")], ("flag", "", 0, "", None)),
    "another ABI of a targeted version is not already supported": (
        body("Cxxdroid", "6.0_x86_64"), [], ("pass", "", 0, "[App Update]: Cxxdroid 6.0_x86_64", None)),
}

failures = 0
for name, (issue_body, issues, expected) in CASES.items():
    result = decide(9, issue_body, issues, PATCHES)
    actual = (result["verdict"], result["reason"], result["duplicate_of"], result["title"], result["retitle"])
    ok = actual == expected
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected}, got {actual}")

comment = decide(9, body("Proton VPN", "5.20.57.0"), [], PATCHES)["comment"]
ok = comment.startswith("<!-- app-update-bot -->\nBundle [v1.49.1](https://github.com/hxreborn/morphe-patches/releases/tag/v1.49.1) "
                         "already supports Proton VPN `5.20.57.0`. This issue is now closed.")
failures += not ok
print(f"{'ok  ' if ok else 'FAIL'} already-targeted comment names the bundle version and the app")

flag_labels = decide(9, body("Nope", "1.0"), [], PATCHES)["labels"]
ok = flag_labels == ["needs info"]
failures += not ok
print(f"{'ok  ' if ok else 'FAIL'} a flag carries the needs info label")

total = len(CASES) + 2
print(f"\n{total - failures}/{total} passed")
sys.exit(1 if failures else 0)
