#!/usr/bin/env python3
"""python3 .github/actions/triage/test_triage.py"""

import json
import subprocess
import sys
import tempfile
from pathlib import Path

SCRIPT = Path(__file__).with_name("triage.py")
APP_REQUEST = Path(__file__).with_name("app_request.py")
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
    "too short": ("close", """### App name

Proton Mail

### App version

7.10.4

### Bug description

app not open
"""),
    "unknown app": ("close", f"""### App name

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
    "unpatched app fails the same way": ("close", f"""### App name

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
     "stars": 42, "targetApps": ["com.duolingo"]},
    {"repo": "other/patches", "name": "Other patches", "source": "gitlab",
     "stars": 7, "targetApps": ["com.other.app"]},
]}

APP_CASES = {
    "app another bundle already patches": (1, "### App\n\nDuolingo\n\n### Package name\n\ncom.duolingo\n"),
    "app nobody patches": (0, "### App\n\nNew\n\n### Package name\n\ncom.nobody.here\n"),
    "package only in the free text": (1, "### App\n\nDuolingo, id=com.duolingo on the store\n"),
    "package read out of a store link": (1, "### App\n\nhttps://play.google.com/store/apps/details?id=com.duolingo\n"),
}


def matches_for(body):
    with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as f:
        f.write(body); bodypath = f.name
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
        json.dump(REGISTRY, f); regpath = f.name
    try:
        out = subprocess.run([sys.executable, str(APP_REQUEST), bodypath, regpath],
                             capture_output=True, text=True, check=True)
        return len(json.loads(out.stdout)["matches"])
    finally:
        Path(bodypath).unlink(); Path(regpath).unlink()


failures = 0
for name, (expected_n, body) in APP_CASES.items():
    actual = matches_for(body)
    ok = actual == expected_n
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected_n} match(es), got {actual}")

for name, (expected, body) in CASES.items():
    actual = verdict_for(*body) if isinstance(body, tuple) else verdict_for(body)
    ok = actual == expected
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}: expected {expected}, got {actual}")

total = len(CASES) + len(APP_CASES)
print(f"\n{total - failures}/{total} passed")
sys.exit(1 if failures else 0)
