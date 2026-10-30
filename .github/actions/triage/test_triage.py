#!/usr/bin/env python3
"""python3 .github/actions/triage/test_triage.py"""

import json
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import app_request
from app_request import decide

SCRIPT = Path(__file__).with_name("triage.py")
BUNDLE_APPS = {"version": "1.60.0", "patches": [{"compatiblePackages": [
    {"name": "AlpineQuest", "packageName": "psyberia.alpinequest.free",
     "targets": [{"version": "2.4.0e"}]},
    {"name": "AtloMaps", "packageName": "com.atlogis.atlomaps",
     "targets": [{"version": "1.0.6"}, {"version": "1.1.0"}, {"version": "2.0.0"}]},
    {"name": "Cx File Explorer", "packageName": "com.cxinventor.file.explorer",
     "targets": [{"version": "2.7.8"}, {"version": "2.7.9"}, {"version": "2.8.1"}]},
    {"name": "Cxxdroid", "packageName": "ru.iiec.cxxdroid",
     "targets": [{"version": "5.6_arm64"}, {"version": "6.0_arm64"}]},
    {"name": "Etsy", "packageName": "com.etsy.android",
     "targets": [{"version": "7.97.0"}, {"version": "7.99.0"}, {"version": "8.0.0"}]},
    {"name": "Fddb", "packageName": "com.fddb",
     "targets": [{"version": "v7.8.4-Build-1-gms-release"}, {"version": "v7.8.5-Build-1-gms-release"}]},
    {"name": "Kick", "packageName": "com.kick.mobile",
     "targets": []},
    {"name": "Magic Earth", "packageName": "com.generalmagic.magicearth",
     "targets": [{"version": "7.1.26.26.21DB1F1B.3C81F7001"}, {"version": "7.1.26.38.B4684D03.6207A61BA"}]},
    {"name": "MovieBox", "packageName": "com.community.oneroom",
     "targets": [{"version": "4.0.03.0918.03"}]},
    {"name": "MovieBox TV", "packageName": "com.community.mbox.tv",
     "targets": [{"version": "1.2.0.0923.web_socialmedia"}]},
    {"name": "Musicolet", "packageName": "in.krosbits.musicolet",
     "targets": [{"version": "6.14.1 build540"}]},
    {"name": "Proton Mail", "packageName": "ch.protonmail.android",
     "targets": [{"version": "7.10.4"}, {"version": "7.11.10"}, {"version": "7.11.5"}, {"version": "7.11.8"}, {"version": "7.11.9"}]},
    {"name": "Proton VPN", "packageName": "ch.protonvpn.android",
     "targets": [{"version": "5.20.39.0"}, {"version": "5.20.57.0"}, {"version": "5.20.74.0"}, {"version": "5.20.98.0"}]},
    {"name": "Quranify", "packageName": "com.mchutov.Quranify",
     "targets": [{"version": "2.2.8"}, {"version": "2.2.9"}, {"version": "2.2.91"}]},
    {"name": "RISE Sleep Tracker", "packageName": "com.risesci.nyx",
     "targets": [{"version": "Android V1.78.47"}, {"version": "Android V1.78.49"}, {"version": "Android V1.78.51"}, {"version": "Android V1.78.52"}, {"version": "Android V1.79.1"}, {"version": "Android V1.79.2"}]},
    {"name": "Rubber Bands", "packageName": "app.rubberbands.fit",
     "targets": [{"version": "3.11"}, {"version": "3.9"}]},
    {"name": "STEEZY", "packageName": "co.steezy.app",
     "targets": [{"version": "5.23.0"}]},
    {"name": "Showly", "packageName": "com.michaldrabik.showly2",
     "targets": [{"version": "3.70.0"}, {"version": "3.72.0"}]},
]}]}

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

ONE_AND_TWO_PATCH_APPS = {"version": "test", "patches": [
    {"compatiblePackages": [{"name": "Etsy", "packageName": "com.etsy.android", "targets": [{"version": "7.97.0"}]}]},
    {"compatiblePackages": [{"name": "Fddb", "packageName": "com.fddb",
                             "targets": [{"version": "v7.8.4-Build-1-gms-release"}]}]},
    {"compatiblePackages": [{"name": "Fddb", "packageName": "com.fddb",
                             "targets": [{"version": "v7.8.4-Build-1-gms-release"}]}]},
]}

PRERELEASE_APPS = {"version": "test", "patches": [{"compatiblePackages": [
    {"name": "Demo", "packageName": "com.demo", "targets": [{"version": "2.0 beta"}]},
    {"name": "Canary Reader", "packageName": "com.canary.reader", "targets": [{"version": "3.1"}]},
]}]}

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
    "no issue form used": ("flag", "the app doesnt open pls fix it\n"),
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
    "dropdown version made of two words": ("pass", f"""### App and version

RISE Sleep Tracker Android V1.78.47

### Bug description

{LONG}
"""),
    "bare number for a target that carries a platform word": ("pass", f"""### App name

RISE Sleep Tracker

### App version

v1.78.47

### Bug description

{LONG}
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
    "patching that did not start needs no error report": ("pass", f"""### App name

Proton Mail

### App version

7.10.4

### What happened

Patching did not start

### Bug description

{LONG}

### Error logs

none
"""),
    "patching that stopped with an error without a report": ("flag", f"""### App name

Proton Mail

### App version

7.10.4

### What happened

Patching started and stopped with an error

### Bug description

{LONG}

### Error logs

_No response_
"""),
    "patching that did not start with a greyed patch needs no error report": ("pass", f"""### App name

Proton Mail

### App version

7.10.4

### What happened

Patching did not start: a patch is greyed out, missing or says Not available

### Bug description

{LONG}

### Error logs

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
    "crash without a logcat": ("flag", f"""### App name

Showly

### App version

3.70.0

### What does not work

The app opens, then crashes

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### What happens

{LONG}

### Error logs

```shell

```
"""),
    "crash with a logcat": ("pass", f"""### App name

Showly

### App version

3.70.0

### What does not work

The app opens, then crashes

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### What happens

{LONG}

### Error logs

```shell
FATAL EXCEPTION: main
```
"""),
    "still locked without trying the unpatched app": ("pass", f"""### App name

Showly

### App version

3.70.0

### What does not work

The patched feature is still locked or still shows ads

### Does the unpatched app do the same thing

I have not tried the unpatched app

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### What happens

{LONG}

### Error logs

[log.txt](https://github.com/user-attachments/files/1/log.txt)
"""),
    "patch missing from the Manager list": ("pass", f"""### App name

Proton Mail

### App version

7.11.10

### What does not work

A patch is missing from the list in Manager

### Does the unpatched app do the same thing

I have not tried the unpatched app

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### What happens

{LONG}

### Error logs

[log.txt](https://github.com/user-attachments/files/1/log.txt)
"""),
    "repackager named without a link asks instead of closing": ("flag", f"""### App and version

Proton Mail 7.10.4

### APK source

APKMirror, not Softonic

### Bug description

{LONG}
"""),
    "repackager link with an uppercase scheme closes": ("close", f"""### App and version

Proton Mail 7.10.4

### APK source

HTTPS://HAPPYMOD.COM/proton-mail

### Bug description

{LONG}
"""),
    "native crash backtrace counts as an error report": ("pass", """### App and version

Proton Mail 7.10.4

### Bug description

Crashes on start.

### Error logs

Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0
backtrace:
  #00 pc 000000000004f2a8  /data/app/lib/arm64/libapp.so
"""),
    "attached report file counts as an error report": ("pass", f"""### What happened

Patching failed

### App and version

Proton Mail 7.10.4

### Bug description

{LONG}

### Error logs

[report.json](https://github.com/user-attachments/files/123/report.json)
"""),
    "build number glued to a target version passes": ("pass", f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.11.9-1234

### Bug description

{LONG}
"""),
    "version typed with Persian digits passes": ("pass", f"""### App and version

Proton Mail (another version, type it below)

### Other version

\u06f7.\u06f1\u06f1.\u06f9

### Bug description

{LONG}
"""),
    "bare version of a suffixed target passes": ("pass", (f"""### App and version

Fddb (another version, type it below)

### Other version

7.8.4

### Bug description

{LONG}
""", ONE_AND_TWO_PATCH_APPS)),
    "another ABI of a targeted version is untargeted": ("flag", f"""### App and version

Cxxdroid (another version, type it below)

### Other version

6.0_armv7

### Bug description

{LONG}
"""),
    "a repackager link beside the real source asks instead of closing": ("flag", f"""### App and version

Proton Mail 7.10.4

### APK source

APKPure: https://apkpure.com/proton-mail/ch.protonmail.android ; not my APK: https://softonic.com/download

### Bug description

{LONG}
"""),
    "prerelease of a target version is untargeted": ("flag", f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.11.9-beta

### Bug description

{LONG}
"""),
    "a beta build of a target version is untargeted": ("flag", f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.11.9 beta

### Bug description

{LONG}
"""),
    "a release of a prerelease-only target is untargeted": ("flag", (f"""### App and version

Demo 2.0

### Bug description

{LONG}
""", PRERELEASE_APPS)),
    "an app named after a prerelease word still matches its target": ("pass", (f"""### App and version

Canary Reader 3.1

### Bug description

{LONG}
""", PRERELEASE_APPS)),
    "device ABI after a universal target version passes": ("pass", f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.11.9 arm64

### Bug description

{LONG}
"""),
    "two-patch app not tried with one patch passes": ("pass", (f"""### App and version

Fddb v7.8.4-Build-1-gms-release

### What does not work

Something else the app used to do stopped working

### Does it still happen with only one patch selected

I have not tried it

### Bug description

{LONG}

### Error logs

[log.txt](https://github.com/user-attachments/files/1/log.txt)
""", ONE_AND_TWO_PATCH_APPS)),
    "steps, expected and actual behaviour count as the description": ("pass", f"""### App name

Showly

### App version

3.70.0

### What does not work

The patched feature is still locked or still shows ads

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### Steps to reproduce

1. Open the app
2. Tap Premium

### Expected behaviour

The premium screen opens unlocked

### Actual behaviour

The paywall shows

### Error logs

[log.txt](https://github.com/user-attachments/files/1/log.txt)
"""),
    "not-working report with no log in the required field": ("flag", f"""### App name

Showly

### App version

3.70.0

### What does not work

The patch has no visible effect

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### Steps to reproduce

{LONG}

### Expected behaviour

No ads

### Actual behaviour

Ads

### Error logs

none
"""),
    "a log linked from a paste site counts as a log": ("pass", f"""### App name

Showly

### App version

3.70.0

### What does not work

The patch has no visible effect

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Does it still happen with only one patch selected

Yes, it happens with only that one patch

### Steps to reproduce

{LONG}

### Expected behaviour

No ads

### Actual behaviour

Ads

### Error logs

https://pastebin.com/Xy12Ab34
"""),
}


def triage(body, patches_list=None):
    with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as f:
        f.write(body)
        path = f.name
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
        json.dump(BUNDLE_APPS if patches_list is None else patches_list, f)
        list_path = f.name
    try:
        out = subprocess.run([sys.executable, str(SCRIPT), path, list_path],
                             capture_output=True, text=True, check=True)
        return json.loads(out.stdout)
    finally:
        Path(path).unlink()
        Path(list_path).unlink()


def verdict_for(body, patches_list=None):
    return triage(body, patches_list)["verdict"]


REGISTRY = {"bundles": [
    {"repo": "someone/patches", "name": "Someone's patches", "source": "github",
     "stars": 42, "targetApps": ["com.duolingo", "ch.protonvpn.android"]},
]}

ACKNOWLEDGED = "### Acknowledgements\n\n- [X] I searched open and closed requests and this isn't a duplicate.\n"

SUPPORTED = {"version": "test", "patches": [{"compatiblePackages": [
    {"name": "MovieBox", "packageName": "com.community.oneroom"},
    {"name": "AtloMaps", "packageName": "com.atlogis.atlomaps"},
    {"name": "AlpineQuest", "packageName": "psyberia.alpinequest.free"},
]}]}

NOW = datetime(2026, 9, 25, tzinfo=timezone.utc)


def request(number, app, package, state="OPEN", comments=()):
    body = f"### App\n\n{app}\n\n### Package name\n\n{package}\n\n### Requested features\n\nUnlock premium\n\n{ACKNOWLEDGED}"
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
        "hold", 0),
    "only the bot commented recently": (
        54, [request(54, "AtloMaps", "com.atlogis.atlomaps", comments=[("github-actions", "2026-09-24T10:00:00Z")])],
        "close", 0),
    "a request without a package name passes": (
        5, [request(5, "Moviebox.ph", "idk")], "pass", 0),
    "package read out of a store link": (
        5, [request(5, "https://play.google.com/store/apps/details?id=com.example.reader", "")], "pass", 0),
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
checks = 0


def record(ok):
    global failures, checks
    checks += 1
    failures += not ok


for name, (number, requests, expected, duplicate_of) in APP_CASES.items():
    result = app_verdict(number, requests)
    actual = (result["verdict"], result["duplicate_of"])
    ok = actual == (expected, duplicate_of)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {(expected, duplicate_of)}, got {actual}")

no_package = app_verdict(5, [request(5, "Moviebox.ph", "idk")])
ok = no_package["package"] == "" and no_package["comment"] == ""
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a website domain is not read as a package name and nothing is posted")

freeform = {"number": 9, "title": "Pixlr AI Art Photo Editor", "state": "OPEN", "comments": [],
            "body": "Remove ads, subscription upsell, AI elements in the homepage. Make a clean homepage."}
no_form = app_verdict(9, [freeform])
ok = (no_form["verdict"], no_form["reason"], no_form["labels"]) == ("close", "no form", ["app request"]) \
    and "category=app-requests" in no_form["comment"] and "missing the App field, the Requested features field " \
    "and the checked acknowledgements." in no_form["comment"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a discussion opened without the form is closed and the reporter is pointed to it")

FORM_CASES = {
    "the form's own output passes": ("pass", "### App\r\n\r\nPixlr https://play.google.com/store/apps/details?id=com.pixlr.express"
                                     "\r\n\r\n### Package name\r\n\r\n_No response_\r\n\r\n### Requested features\r\n\r\n"
                                     "Remove ads\r\n\r\n### Additional info\r\n\r\n_No response_\r\n\r\n### Acknowledgements"
                                     "\r\n\r\n- [X] One\r\n- [X] Two\r\n- [X] Three\r\n- [X] Four"),
    "an older form with two acknowledgements passes": ("pass", "### App\n\nPixlr\n\n### Requested features\n\nRemove ads\n\n"
                                                       "### Acknowledgements\n\n- [x] One\n- [x] Two"),
    "form labels written as bold text": ("close", "**App:**\nPixlr https://play.google.com/store/apps/details?id=com.pixlr.express"
                                         "\n\n**Requested features:**\n- Remove ads\n\n**Acknowledgements**\n- [ ]  One\n- [ ]  Two"),
    "an App heading alone": ("close", "### App\n\nPixlr\n\nRemove ads"),
    "requested features left empty": ("close", "### App\n\nPixlr\n\n### Requested features\n\n_No response_\n\n" + ACKNOWLEDGED),
    "an acknowledgement left unchecked": ("close", "### App\n\nPixlr\n\n### Requested features\n\nRemove ads\n\n"
                                          "### Acknowledgements\n\n- [X] One\n- [ ] Two"),
    "acknowledgements missing": ("close", "### App\n\nPixlr\n\n### Requested features\n\nRemove ads\n"),
}
for name, (expected, body) in FORM_CASES.items():
    actual = app_verdict(9, [{**freeform, "body": body}])["verdict"]
    record(actual == expected)
    print(f"{'ok  ' if actual == expected else 'FAIL'} {name}: expected {expected}, got {actual}")

partial = app_verdict(9, [{**freeform, "body": "### App\n\nPixlr\n\n### Requested features\n\nRemove ads\n"}])["comment"]
ok = "so it's missing the checked acknowledgements." in partial
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the close comment names only what is missing")

by_owner = app_verdict(9, [{**freeform, "association": "OWNER"}])
by_collaborator = app_verdict(9, [{**freeform, "association": "COLLABORATOR"}])
by_other = app_verdict(9, [{**freeform, "association": "NONE"}])
ok = by_owner["verdict"] == "pass" and by_collaborator["verdict"] == "pass" and by_other["verdict"] == "close"
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a maintainer's own thread without the form is left open")

registry_hit = app_verdict(9, [request(9, "Proton VPN", "ch.protonvpn.android")])
ok = (registry_hit["matches"] == ["someone/patches"] and registry_hit["verdict"] == "flag"
      and registry_hit["labels"] == ["needs info", "other bundle"]
      and "[Someone's patches](https://github.com/someone/patches) already patches `ch.protonvpn.android`. "
          "To keep work from being split across bundles" in registry_hit["comment"])
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} app another bundle already patches asks which features it lacks, not closed")


def posted(repo, number, title, body=""):
    return {"repo": repo, "number": number, "title": title, "body": body}


def cross_posted(app, package, elsewhere, registry=REGISTRY, previous=""):
    this = request(9, app, package)
    return decide(9, this["body"], [this], SUPPORTED, registry, this["title"], NOW, elsewhere, previous)


musicolet = cross_posted("Musicolet", "", [posted("someone/patches", 1099, "feat: new app [Musicolet]")])
ok = (musicolet["verdict"], musicolet["labels"], musicolet["cross_posts"]) == ("flag", ["other bundle"], ["someone/patches#1099"]) \
    and "This app is also requested at [someone/patches#1099](https://github.com/someone/patches/issues/1099). " in musicolet["comment"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the requester's issue in a listed bundle naming the app is linked and labelled")

by_package = cross_posted("Some Reader", "com.some.reader", [posted("someone/patches", 5, "Request", "Package: com.some.reader")])
ok = by_package["cross_posts"] == ["someone/patches#5"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a cross-post naming the package in its body is found")

typos = [cross_posted("Fawesome", "", [posted("someone/patches", 6, "[Request] Fawesom TV")]),
         cross_posted("Fawesom Free Movies and TV", "", [posted("someone/patches", 6, "[App Request] Fawesome Movies and TV")])]
linked = request(9, "https://play.google.com/store/apps/details?id=com.future.fawesome", "")
linked["title"] = "[App Request]: Fawesom Free Movies and TV"
typos.append(decide(9, linked["body"], [linked], SUPPORTED, REGISTRY, linked["title"], NOW,
                    [posted("someone/patches", 6, "[App Request] Fawesome Movies and TV")]))
ok = [t["cross_posts"] for t in typos] == [["someone/patches#6"]] * 3
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a brand one letter short on either side matches, read from the title when the App field is a link")

misses = [
    cross_posted("Period Tracker", "", [posted("someone/patches", 7, "[Request] Tasker: remove trial period")]),
    cross_posted("Musicolet", "", [posted("someone/patches", 8, "Music player with lyrics")]),
    cross_posted("Mbox Phone", "com.community.mbox", [posted("someone/patches", 9, "TV", "com.community.mbox.tv")]),
    cross_posted("Musicolet", "", [posted("stranger/kodi-addon", 10, "Musicolet support")]),
    cross_posted("Musicolet", "", [posted("someone/patches", 11, "[Musicolet]")], registry=None),
]
ok = [m["cross_posts"] for m in misses] == [[]] * len(misses) and [m["verdict"] for m in misses] == ["pass"] * len(misses)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a word deeper in the title, a generic word, a longer package, an unlisted repo "
      f"and a missing registry match nothing")

lookalikes = [
    cross_posted("Proton Drive", "", [posted("someone/patches", 13, "[App Request] Proton VPN")]),
    cross_posted("Google Pay", "", [posted("someone/patches", 14, "[App Request]: Google Maps")]),
    cross_posted("Notion", "", [posted("someone/patches", 15, "Notions of movement")]),
    cross_posted("Gallery Vault", "", [posted("someone/patches", 16, "Gallery crashes on open")]),
    cross_posted("The Weather Channel", "", [posted("someone/patches", 17, "Disney Channel")]),
]
ok = [m["cross_posts"] for m in lookalikes] == [[]] * len(lookalikes)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} apps sharing a vendor or a common first word, and a plural, are not cross-posts")

reports = [
    cross_posted("Musicolet", "", [posted("someone/patches", 18, "bug: Musicolet crashes after patching")]),
    cross_posted("Musicolet", "in.krosbits.musicolet",
                 [{**posted("someone/patches", 19, "Musicolet", "in.krosbits.musicolet"), "labels": ["Bug"]}]),
]
ok = [m["cross_posts"] for m in reports] == [[], []]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a bug report about the app in another bundle is not a request")

earlier = "<!-- app-request-bot -->\nThis app was also requested at " \
          "[someone/patches#1099](https://github.com/someone/patches/issues/1099), " \
          "[stranger/kodi#3](https://github.com/stranger/kodi/issues/3)."
kept = cross_posted("Musicolet", "", [], previous=earlier)
ok = kept["cross_posts"] == ["someone/patches#1099"] and \
    "requested at [someone/patches#1099](https://github.com/someone/patches/issues/1099). " in kept["comment"] \
    and "stranger/kodi" not in kept["comment"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a cross-post found earlier is kept after it leaves the event feed, unlisted repos dropped")

app_request.REPO = "someone/morphe-patches"
own = cross_posted("Musicolet", "", [posted("someone/patches", 12, "[Musicolet]")])
app_request.REPO = ""
ok = own["cross_posts"] == []
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} another repo of this bundle's owner is not a cross-post")

for given, expected in {"privacy notes": "privacy notes",
                        "[App Request]: Gboard": "Gboard",
                        "[app request]:Gboard": "Gboard",
                        "[App Request]: ": "Gboard"}.items():
    actual = app_verdict(9, [{**request(9, "Gboard", ""), "title": given}])["title"]
    ok = actual == expected
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} title {given!r} becomes {expected!r}, got {actual!r}")

untargeted = triage(f"""### App and version

Proton Mail (another version, type it below)

### Other version

7.5.6 (388)

### Bug description

{LONG}
""")["comment"]
ok = "but not `7.5.6 (388)` yet" in untargeted
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} untargeted version quotes only the typed version")

hostile = triage(f"""### App name

Zzz` @octocat [Install the fix](https://example.com/fix.apk)

### App version

1.0

### Bug description

{LONG}
""")["comment"]
ok = "`Zzz' @octocat [Install the fix](https://example.com/fix.apk) 1.0`" in hostile
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a reported app name stays inside one code span")

eg_requests = [{**request(n, app, ""), "body": request(n, app, "")["body"] + "\n### Additional info\n\nRemove ads, e.g. banners\n"}
               for n, app in ((100, "Foo Player"), (101, "Bar Notes"))]
prose_package = {**request(5, "New Quran App", ""), "body": request(5, "New Quran App", "")["body"]
                 + "\n### Additional info\n\nLike the patch for com.atlogis.atlomaps\n"}
linked_package = {**request(6, "GearUP", ""), "body": request(6, "GearUP", "")["body"]
                  + "\n### Additional info\n\nLike https://play.google.com/store/apps/details?id=com.atlogis.atlomaps\n"}
foreign_link = request(7, "Unknown https://example.com/a?id=com.atlogis.atlomaps", "")
no_app_field = {**request(8, "x", ""), "body": "### Details\n\nhttps://play.google.com/store/apps/details?id=com.atlogis.atlomaps\n"}
ok = [app_verdict(101, eg_requests)["verdict"], app_verdict(5, [prose_package])["verdict"],
      app_verdict(6, [linked_package])["verdict"], app_verdict(7, [foreign_link])["verdict"]] == ["pass"] * 4 \
    and app_verdict(8, [no_app_field])["package"] == ""
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} only a store link in the App field is read as its package")

for app, expected in {"Duolingo, https://play.google.com/store/apps/details?id=com.duolingo": "Duolingo",
                      "https://play.google.com/store/apps/details?id=com.example.reader": "com.example.reader",
                      "[Magic Earth](https://play.google.com/store/apps/details?id=com.generalmagic.magicearth)":
                          "Magic Earth"}.items():
    actual = app_verdict(9, [{**request(9, app, ""), "title": "[App Request]:"}])["title"]
    ok = actual == expected
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} empty title takes {expected!r} from the App field, got {actual!r}")

actual = [app_verdict(9, [{**request(9, "Rubber Bands", ""), "title": title}])["title"]
          for title in ("App Request: Rubber Bands", "[Feature]: Rubber Bands", "[App Request]\uff1aRubber Bands")]
ok = actual == ["Rubber Bands"] * 3
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} other prefix spellings and another form's tag are replaced, got {actual!r}")

hostile_registry = {"bundles": [{"repo": "evil/patches", "name": "x](https://example.com) @octocat [y",
                                 "targetApps": ["ch.protonvpn.android"]},
                                {"repo": "hxreborn/morphe-patches", "name": "Hxreborn", "targetApps": ["ch.protonvpn.android"]}]}
app_request.REPO = "hxreborn/morphe-patches"
this = request(9, "Proton VPN", "ch.protonvpn.android")
listed = decide(9, this["body"], [this], SUPPORTED, hostile_registry, this["title"], NOW)
app_request.REPO = ""
ok = listed["matches"] == ["evil/patches"] and "](https://example.com)" not in listed["comment"] and "@" not in listed["comment"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} registry names are stripped of markup and this bundle is not listed")

started = datetime.now(timezone.utc)
slow_inputs = [verdict_for("### App and version\n\n" + "1" * 65000),
               verdict_for("### App and version\n\nProton Mail" + " " * 65000 + "x\n\n### Bug description\n\nx\n")]
ok = (datetime.now(timezone.utc) - started).total_seconds() < 5
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} long runs of digits and spaces in the app field are read in linear time")

declared_domain = app_verdict(9, [request(9, "Moviebox", "moviebox.ph")])
long_title = app_verdict(9, [{**request(9, "Gboard", ""), "title": "App Request: " + "x" * 300}])["title"]
SUPPORTED["patches"][0]["compatiblePackages"].append({"name": "Quranify", "packageName": "com.mchutov.Quranify"})
mixed_case = app_verdict(9, [request(9, "Quranify", "com.MCHUTOV.quranify")])
SUPPORTED["patches"][0]["compatiblePackages"].pop()
ok = (mixed_case["verdict"], declared_domain["package"], len(long_title)) == ("close", "", 256)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} package case is ignored, a declared website is not a package, titles stop at 256")

bad_repo = {"bundles": [{"repo": "evil/patches) [x](https://example.com", "name": "x", "targetApps": ["ch.protonvpn.android"]}]}
this = request(9, "Proton VPN", "ch.protonvpn.android")
ok = decide(9, this["body"], [this], SUPPORTED, bad_repo, this["title"], NOW)["matches"] == []
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a registry entry whose repo is not owner/name is ignored")

fullwidth = app_verdict(9, [request(9, "\uff21\uff54\uff4c\uff4f\uff2d\uff41\uff50\uff53", "")])
ok = fullwidth["verdict"] == "flag" and fullwidth["labels"] == ["duplicate"]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a fullwidth name still matches the supported app it imitates")


def by(login, number, app, state="OPEN", labels=()):
    return {**request(number, app, ""), "state": state, "author": {"login": login},
            "labels": [{"name": label} for label in labels]}


def limited_verdict(number, requests, enforce_limit=True):
    this = next(r for r in requests if r["number"] == number)
    return decide(number, this["body"], requests, SUPPORTED, None, this["title"], NOW, enforce_limit=enforce_limit)


busy = [by("busy", 1, "Foo Player"), by("busy", 2, "Bar `Notes`", labels=("needs info",)), by("busy", 3, "Baz Reader"),
        by("busy", 4, "Old App", state="CLOSED"), by("other", 5, "Qux Maps"), by("busy", 6, "Zed Radio")]

limit_closed = limited_verdict(6, busy)
ok = ((limit_closed["verdict"], limit_closed["reason"], limit_closed["labels"]) == ("close", "not planned", [])
      and "[#2](https://redirect.github.com//discussions/2) `[App Request]: Bar 'Notes'`" in limit_closed["comment"]
      and "#4" not in limit_closed["comment"] and "#5" not in limit_closed["comment"]
      and limit_closed["comment"].count("\n- [#") == 3)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a fourth open request closes as not planned and lists the three open ones, needs info included")

under = [limited_verdict(6, [r for r in busy if r["number"] != 3]), limited_verdict(6, busy, enforce_limit=False),
         limited_verdict(5, busy)]
ok = [u["verdict"] for u in under] == ["pass"] * 3
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} two other open requests, an exempt author and another person's request are not limited")

for name, (expected, body) in CASES.items():
    actual = verdict_for(*body) if isinstance(body, tuple) else verdict_for(body)
    ok = actual == expected
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected}, got {actual}")

print(f"\n{checks - failures}/{checks} passed")
sys.exit(1 if failures else 0)
