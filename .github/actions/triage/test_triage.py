#!/usr/bin/env python3
"""python3 .github/actions/triage/test_triage.py"""

import json
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from app_request import decide

SCRIPT = Path(__file__).with_name("triage.py")
PATCHES_LIST = Path(__file__).parents[3] / "patches-list.json"

LONG = "Patching stops with an error dialog partway through and never writes an output file."

COLLIDING_APPS = {
    "version": "test",
    "patches": [{"compatiblePackages": [
        {"name": "Block Blast!", "packageName": "com.block.blast",
         "targets": [{"version": "10.4.5"}]},
        {"name": "Block Puzzle", "packageName": "com.block.puzzle",
         "targets": [{"version": "6.0"}]},
    ]}],
}

CASES = {
    "version a target is a substring of": ("flag", f"""### App name

Proton Mail

### App version

7.10.44

### Bug description

{LONG}
"""),
    "version written with a v prefix": ("pass", f"""### App name

Cx File Explorer

### App version

v2.7.8

### Bug description

{LONG}
"""),
    "app sharing a word with another app": ("pass", (f"""### App name

block blast

### App version

10.4.5

### Bug description

{LONG}
""", COLLIDING_APPS)),
    "app and version picked from the dropdown": ("pass", f"""### App and version

Proton Mail 7.10.4

### Other version

_No response_

### Bug description

{LONG}
"""),
    "another version picked but not typed": ("flag", f"""### App and version

Proton Mail (another version, type it below)

### Other version

_No response_

### Bug description

{LONG}
"""),
    "another version typed in": ("flag", f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.12.0

### Bug description

{LONG}
"""),
    "APK from a repackager": ("close", f"""### App name

Proton Mail

### App version

7.10.4

### APK source

https://proton-mail.en.softonic.com/android

### Bug description

{LONG}
"""),
    "no issue form used": ("close", "the app doesnt open pls fix it\n"),
    "too short": ("flag", """### App name

Proton Mail

### App version

7.10.4

### Bug description

app not open
"""),
    "unknown app": ("flag", f"""### App name

Duolingo

### App version

5.1.2

### Bug description

{LONG}
"""),
    "fenced heading in log": ("pass", f"""### App name

Proton Mail

### App version

7.10.4

### What happened

Patching failed

### Bug description

{LONG}

### Debug log

```shell
### Applying patches
app.morphe.patcher.patch.PatchException: Failed to match
```
"""),
    "short description with the error report attached": ("pass", """### App and version

Proton Mail 7.10.4

### Other version

_No response_

### What happened

Patching failed

### Bug description

Fail to patch giving this error below.
Thanks!

### Error logs

Manager: 1.32.0
Patcher: 1.14.1

app.morphe.patcher.patch.PatchException: no native library found
"""),
    "markdown heading inside the description": ("pass", f"""### App and version

Proton Mail 7.10.4

### Bug description

Install fails.
## Steps
{LONG}
"""),
    "unclosed code fence in another field": ("pass", f"""### APK source

```apkmirror

### App and version

Proton Mail 7.10.4

### Bug description

{LONG}
"""),
    "tilde fence around the log": ("pass", """### App and version

Proton Mail 7.10.4

### Bug description

Fail to patch giving this error below.

### Error logs

~~~
### Applying patches
app.morphe.patcher.patch.PatchException: no native library found
~~~
"""),
    "repackager named in the description only": ("pass", f"""### App and version

Proton Mail 7.10.4

### APK source

https://www.apkmirror.com/apk/proton-technologies-ag/

### Bug description

{LONG} I did not use softonic.
"""),
    "source host that merely ends like a repackager": ("pass", f"""### App and version

Proton Mail 7.10.4

### APK source

https://titan1.com/proton-mail

### Bug description

{LONG}
"""),
    "bare logcat with a short description": ("pass", """### App and version

Proton Mail 7.10.4

### Bug description

Crashes on start.

### Error logs

09-30 10:00:01 E/AndroidRuntime: FATAL EXCEPTION: main
java.lang.NullPointerException
\tat app.x.Y.z(Y.java:1)
"""),
    "localized manager report without a marker word": ("pass", """### App and version

Proton Mail 7.10.4

### Bug description

Не удалось пропатчить.

### Error logs

Менеджер: 1.32.0
Android: 16 (API 36)
Память: 2.01 GB / 7.85 GB
"""),
    "long description in a script without spaces": ("pass", """### App and version

Proton Mail 7.10.4

### Bug description

修补过程在中途停止并且显示一个错误对话框，最终没有生成任何输出文件，我已经尝试了多次并且每次都是同样的结果，请帮忙看看。
"""),
    "junk in the log field": ("flag", """### App and version

Proton Mail 7.10.4

### Bug description

it broke

### Error logs

asdf
"""),
    "patching failed without a report": ("flag", f"""### App name

Proton Mail

### App version

7.10.4

### What happened

Patching failed

### Bug description

{LONG}

### Debug log

_No response_
"""),
    "version the bundle does not target": ("flag", f"""### App name

Proton Mail

### App version

99.0.0

### Bug description

{LONG}
"""),
    "app targeting any version": ("pass", f"""### App name

Kick

### App version

99.99

### Bug description

{LONG}
"""),
    "runtime report skipping the checks": ("flag", f"""### App name

Showly

### App version

3.70.0

### Does the unpatched app do the same thing

I have not tried the unpatched app

### What happens

{LONG}
"""),
    "unpatched app fails the same way": ("flag", f"""### App name

Showly

### App version

3.70.0

### Does the unpatched app do the same thing

The unpatched app fails the same way

### What happens

{LONG}
"""),
    "complete runtime report": ("pass", f"""### App name

Showly

### App version

3.70.0

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### What happens

{LONG}
"""),
}


def verdict_for(body, patches_list=None):
    with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as f:
        f.write(body)
        path = f.name
    list_path = str(PATCHES_LIST)
    extra = None
    if patches_list is not None:
        with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
            json.dump(patches_list, f)
            extra = list_path = f.name
    try:
        out = subprocess.run([sys.executable, str(SCRIPT), path, list_path],
                             capture_output=True, text=True, check=True)
        return json.loads(out.stdout)["verdict"]
    finally:
        Path(path).unlink()
        if extra:
            Path(extra).unlink()


REGISTRY = {"bundles": [
    {"repo": "someone/patches", "name": "Someone's patches", "source": "github",
     "stars": 42, "targetApps": ["com.duolingo", "ch.protonvpn.android"]},
]}

SUPPORTED = {"version": "test", "patches": [{"compatiblePackages": [
    {"name": "MovieBox", "packageName": "com.community.oneroom"},
    {"name": "AtloMaps", "packageName": "com.atlogis.atlomaps"},
    {"name": "AlpineQuest", "packageName": "psyberia.alpinequest.free"},
]}]}

NOW = datetime(2026, 9, 25, tzinfo=timezone.utc)


def request(number, app, package, state="OPEN", comments=()):
    body = f"### App\n\n{app}\n\n### Package name\n\n{package}\n\n### Requested features\n\nUnlock premium\n"
    return {"number": number, "title": f"[App Request]: {app}", "body": body, "state": state,
            "comments": [{"author": {"login": who}, "createdAt": when} for who, when in comments]}


MOVIEBOX_TV = [
    request(76, "https://moviebox.ph/moviebox-tv-apk", "com.community.mbox.tv"),
    request(82, "MovieBox TV, https://h5-api.aoneroom.com/pkg/redirect?a=MovieBoxTV&p=com.community.mbox.tv&c=1",
            "com.community.mbox.tv"),
    request(85, "Moviebox.ph", "com.community.mbox.tv"),
]

APP_CASES = {
    "first of three MovieBox TV requests is kept open": (76, MOVIEBOX_TV, "pass", 0),
    "second MovieBox TV request closes as a duplicate": (82, MOVIEBOX_TV, "close", 76),
    "third MovieBox TV request closes as a duplicate of the first": (85, MOVIEBOX_TV, "close", 76),
    "MovieBox TV is not the supported MovieBox phone app": (
        1, [request(1, "Moviebox for Android TV", "com.community.mbox.tv")], "pass", 0),
    "package only in an aoneroom redirect link": (
        82, MOVIEBOX_TV[:1] + [request(82, "MovieBox TV, https://h5-api.aoneroom.com/pkg/redirect?p=com.community.mbox.tv", "see link")],
        "close", 76),
    "request for an app already supported here": (
        74, [request(74, "AltoMaps, https://play.google.com/store/apps/details?id=com.atlogis.atlomaps",
                     "com.atlogis.atlomaps")], "close", 0),
    "supported app with a recent human comment is left alone": (
        54, [request(54, "AtloMaps", "com.atlogis.atlomaps", comments=[("someone", "2026-09-23T10:00:00Z")])],
        "pass", 0),
    "only the bot commented recently": (
        54, [request(54, "AtloMaps", "com.atlogis.atlomaps", comments=[("github-actions", "2026-09-24T10:00:00Z")])],
        "close", 0),
    "a website domain is not a package name": (
        5, [request(5, "Moviebox.ph", "idk")], "flag", 0),
    "package read out of a store link": (
        5, [request(5, "https://play.google.com/store/apps/details?id=com.duolingo", "")], "pass", 0),
    "earlier request closed, not reopened silently": (
        9, [request(3, "Gboard", "com.google.android.inputmethod.latin", state="CLOSED"),
            request(9, "Gboard", "com.google.android.inputmethod.latin")], "flag", 0),
    "same name as a supported app, different package": (
        9, [request(9, "AlpineQuest", "psyberia.alpinequest.full")], "flag", 0),
    "same name as an open request, no package": (
        9, [request(4, "STEEZY", "co.steezy.app"), request(9, "Steezy", "")], "flag", 0),
    "a later request never closes an earlier one": (
        82, MOVIEBOX_TV[1:], "pass", 0),
    "unrelated apps sharing a word are not duplicates": (
        9, [request(4, "Proton VPN", "ch.protonvpn.android"), request(9, "Proton Drive", "me.proton.android.drive")],
        "pass", 0),
}


def app_verdict(number, requests):
    this = next(r for r in requests if r["number"] == number)
    return decide(number, this["body"], requests, SUPPORTED, REGISTRY, this["title"], NOW)


failures = 0
for name, (number, requests, expected, duplicate_of) in APP_CASES.items():
    result = app_verdict(number, requests)
    actual = (result["verdict"], result["duplicate_of"])
    ok = actual == (expected, duplicate_of)
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {(expected, duplicate_of)}, got {actual}")

registry_hit = app_verdict(9, [request(9, "Proton VPN", "ch.protonvpn.android")])
ok = registry_hit["matches"] == ["someone/patches"] and "Someone's patches" in registry_hit["comment"]
failures += not ok
print(f"{'ok  ' if ok else 'FAIL'} app another bundle already patches is listed, not closed")

for name, (expected, body) in CASES.items():
    actual = verdict_for(*body) if isinstance(body, tuple) else verdict_for(body)
    ok = actual == expected
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected}, got {actual}")

total = len(CASES) + len(APP_CASES) + 1
print(f"\n{total - failures}/{total} passed")
sys.exit(1 if failures else 0)
