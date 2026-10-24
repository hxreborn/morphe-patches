#!/usr/bin/env python3
"""python3 .github/scripts/bump_prs.py app-updates/app-updates.json"""

import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

from versions import APP_UPDATE_TITLE_PREFIX, build_tags, compare

ISSUE_LABEL = "app-update"
COMPAT_PATH = "patches/src/main/kotlin/app/morphe/patches/shared/compat/AppCompatibilities.kt"
TARGET = re.compile(r'AppTarget\(version = "([^"]+)"(?:, versionCode = (\d+))?(?:, minSdk = (\d+))?\)')
SAFE_VERSION = re.compile(r"[\w .+()-]+")
BOT_EMAIL = "41898282+github-actions[bot]@users.noreply.github.com"
BOT_IDENTITY = {"GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_AUTHOR_EMAIL": BOT_EMAIL,
                "GIT_COMMITTER_NAME": "github-actions[bot]", "GIT_COMMITTER_EMAIL": BOT_EMAIL}


def gh(*args, **kwargs):
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True, **kwargs).stdout


def git(*args, **kwargs):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, **kwargs).stdout


def format_target(version, version_code, min_sdk):
    fields = [f'version = "{version}"']
    fields += [f"versionCode = {version_code}"] if version_code is not None else []
    fields += [f"minSdk = {min_sdk}"] if min_sdk is not None else []
    return f"AppTarget({', '.join(fields)})"


def add_target(source, package, version, version_code, min_sdk):
    if not SAFE_VERSION.fullmatch(version):
        raise ValueError(f"version {version!r} has characters a Kotlin string or subject cannot carry as is")
    if any(value is not None and type(value) is not int for value in (version_code, min_sdk)):
        raise ValueError(f"version code {version_code!r} or min SDK {min_sdk!r} is not a number")
    lines = source.split("\n")
    declared = [i for i, line in enumerate(lines) if line == f'        packageName = "{package}",']
    if len(declared) != 1:
        raise ValueError(f"{len(declared)} declarations of {package}")
    end = next((i for i in range(declared[0], len(lines)) if lines[i] == "    )"), len(lines))
    start = next((i for i in range(declared[0], -1, -1) if lines[i].endswith(" = Compatibility(")), 0)
    found = [i for i in range(start, end) if lines[i].startswith("        targets = ")]
    if len(found) != 1:
        raise ValueError("no single targets list")
    first = found[0]
    inline = re.fullmatch(r"        targets = listOf\((.+)\),", lines[first])
    if inline:
        entries, last = [inline.group(1)], first
    elif lines[first] == "        targets = listOf(":
        last = next((i for i in range(first, end) if lines[i] == "        ),"), None)
        if last is None or not all(re.fullmatch(r"            \S.*,", line) for line in lines[first + 1:last]):
            raise ValueError("targets list is not one entry per line")
        entries = [line.strip()[:-1] for line in lines[first + 1:last]]
    else:
        raise ValueError("targets is not a listOf call")
    parsed = [TARGET.fullmatch(entry) for entry in entries]
    if not parsed or not all(parsed):
        raise ValueError("targets are not plain AppTarget entries")
    versions = [m.group(1) for m in parsed]
    if any(compare(v, version) == 0 for v in versions):
        raise ValueError(f"{version} is already targeted")
    steps = [compare(a, b) for a, b in zip(versions, versions[1:])]
    descending = bool(steps) and all(step > 0 for step in steps)
    if not descending and not all(step < 0 for step in steps):
        raise ValueError("targets are not sorted by version")
    newest = parsed[0 if descending else -1]
    if compare(version, newest.group(1)) <= 0 or build_tags(version) != build_tags(newest.group(1)):
        raise ValueError(f"{version} does not follow {newest.group(1)}")
    if (newest.group(2) and version_code is None) or (newest.group(3) and min_sdk is None):
        raise ValueError("version code or min SDK unknown")
    added = format_target(version, version_code if newest.group(2) else None, min_sdk if newest.group(3) else None)
    entries = [added, *entries] if descending else [*entries, added]
    block = ["        targets = listOf(", *(f"            {entry}," for entry in entries), "        ),"]
    return "\n".join(lines[:first] + block + lines[last + 1:])


def bump_apk(record):
    if record.get("readiness") != "ready":
        return None
    attempts = record.get("verification", {}).get(record["candidate"], [])
    checked = next((a for a in attempts if a.get("patch_check")), {})
    if checked.get("package_ok") and checked.get("version_ok") and checked.get("cert_ok") is not False:
        return checked
    return None


def bump_plan(subject, branch_subject, prs):
    push = branch_subject != subject
    open_pr = next((pr for pr in prs if pr["state"] == "OPEN"), None)
    if open_pr:
        return push, ("edit", open_pr["number"]) if push and open_pr["title"] != subject else None
    if not push and any(pr["title"] == subject for pr in prs):
        return push, None
    return push, ("create", None)


def bump_commit(text, subject, tracker):
    blob = git("hash-object", "-w", "--stdin", input=text).strip()
    env = {**os.environ, **BOT_IDENTITY}
    with tempfile.TemporaryDirectory() as temp:
        index = {**env, "GIT_INDEX_FILE": str(Path(temp) / "index")}
        git("read-tree", "origin/dev", env=index)
        git("update-index", "--cacheinfo", f"100644,{blob},{COMPAT_PATH}", env=index)
        tree = git("write-tree", env=index).strip()
    footer = ["-m", f"Closes #{tracker}"] if tracker else []
    return git("commit-tree", tree, "-p", "origin/dev", "-m", subject, *footer, env=env).strip()


def sync_bump_prs(records):
    git("fetch", "-q", "--depth=1", "origin", "+refs/heads/dev:refs/remotes/origin/dev")
    source = git("show", f"origin/dev:{COMPAT_PATH}")
    issues = json.loads(gh("issue", "list", "--label", ISSUE_LABEL, "--state", "open", "--limit", "1000",
                           "--json", "number,title"))
    for record in records:
        apk = bump_apk(record)
        if not apk:
            continue
        name = record["name"]
        try:
            text = add_target(source, record["package"], apk["version"], apk.get("version_code"), apk.get("min_sdk"))
        except ValueError as error:
            print(f"{name}: no bump PR, {error}", file=sys.stderr)
            continue
        branch = f"app-update/{record['package']}"
        subject = f"bump({name}): add support for `{apk['version']}`"
        tracker_title = f"{APP_UPDATE_TITLE_PREFIX}{name} {record['candidate']}"
        tracker = next((i["number"] for i in issues if i["title"] == tracker_title), None)
        branch_subject = None
        if git("ls-remote", "--heads", "origin", f"refs/heads/{branch}").strip():
            git("fetch", "-q", "--depth=1", "origin", f"refs/heads/{branch}")
            author, branch_subject = git("log", "-1", "--format=%ae%n%s", "FETCH_HEAD").strip().split("\n", 1)
            if author != BOT_EMAIL:
                print(f"{name}: {branch} has commits by {author}, left alone", file=sys.stderr)
                continue
        prs = json.loads(gh("pr", "list", "--head", branch, "--state", "all", "--json", "number,title,state"))
        push, pr = bump_plan(subject, branch_subject, prs)
        if push:
            git("push", "-q", "--force", "origin", f"{bump_commit(text, subject, tracker)}:refs/heads/{branch}")
        body = f"Applies to {name} {apk['version']}. Needs a device check before merge."
        if pr and pr[0] == "create":
            gh("pr", "create", "--draft", "--base", "dev", "--head", branch, "--title", subject, "--body", body)
        elif pr:
            gh("pr", "edit", str(pr[1]), "--title", subject, "--body", body)
        print(f"{name}: {branch} {'pushed' if push else 'unchanged'}, pr {pr[0] if pr else 'unchanged'}",
              file=sys.stderr)


if __name__ == "__main__":
    sync_bump_prs(json.loads(Path(sys.argv[1]).read_text())["apps"])
