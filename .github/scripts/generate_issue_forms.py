#!/usr/bin/env python3
"""python3 generate_issue_forms.py [patches-list.json]"""

import json
import re
import subprocess
import sys
from pathlib import Path

list_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("patches-list.json")
forms = [Path(".github/ISSUE_TEMPLATE/bug_report.yml"), Path(".github/ISSUE_TEMPLATE/not_working.yml")]

ABI_NAMES = {"ARM64_V8A": "arm64-v8a", "ARMEABI_V7A": "armeabi-v7a", "X86_64": "x86_64", "X86": "x86"}
RECENT_RELEASES = 6

data = json.loads(list_path.read_text(encoding="utf-8"))
apps, patches, abis, targets = set(), set(), set(), {}
for patch in data["patches"]:
    for package in patch.get("compatiblePackages") or []:
        apps.add(package["name"])
        patches.add(f'{package["name"]} · {patch["name"]}')
        versions = targets.setdefault(package["name"], set())
        for target in package.get("targets") or []:
            if target.get("version"):
                versions.add(target["version"])
            abis.update((target.get("versionCodes") or {}).keys())

app_versions = []
for app in sorted(targets, key=str.lower):
    app_versions += [f"{app} {v}" for v in sorted(targets[app])]
    app_versions.append(f"{app} (another version, type it below)")

tags = subprocess.run(["git", "tag", "--sort=-v:refname"], capture_output=True, text=True).stdout.split()
versions = [data["version"]] + [t.lstrip("v") for t in tags if t.lstrip("v") != data["version"]]

sets = {
    "apps": sorted(apps, key=str.lower),
    "targets": app_versions,
    "patches": sorted(patches, key=str.lower) + ["Not narrowed down yet"],
    "abis": [ABI_NAMES.get(a, a.lower()) for a in sorted(abis)] + ["All of them (universal)", "Don't know"],
    "versions": versions[:RECENT_RELEASES] + ["Older than these"],
}

def render(key):
    return "".join(f"        - {json.dumps(item)}\n" for item in sets[key])

for form in forms:
    text = form.read_text(encoding="utf-8")
    def replace(match):
        return f"{match.group(1)}{render(match.group(2))}{match.group(3)}"
    new = re.sub(r"(        # generated:(\w+)\n)(?:        - .*\n)*(        # /generated\n)", replace, text)
    if new != text:
        form.write_text(new, encoding="utf-8")
    print(f"{form.name}: {'updated' if new != text else 'unchanged'}")
