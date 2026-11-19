#!/usr/bin/env python3
"""python3 .github/scripts/test_bump_prs.py"""

import json
import os
import subprocess
import tempfile
from pathlib import Path

import bump_prs

SOURCE = """internal object AppCompatibilities {
    val ONE = Compatibility(
        name = "One",
        packageName = "com.one",
        targets = listOf(AppTarget(version = "1.0", versionCode = 10, minSdk = 24)),
    )

    val TWO = Compatibility(
        name = "Two",
        packageName = "com.two",
        signatures = setOf(
            "aa",
        ),
        targets = listOf(
            AppTarget(version = "2.0", versionCode = 20, minSdk = 24),
            AppTarget(version = "2.1", versionCode = 21, minSdk = 24),
        ),
    )

    val NEWEST_FIRST = Compatibility(
        name = "Newest first",
        packageName = "com.newest",
        targets = listOf(
            AppTarget(version = "Android V1.79.1", minSdk = 26),
            AppTarget(version = "Android V1.78.52", minSdk = 26),
        ),
    )

    val SHARED = Compatibility(
        name = "Shared",
        packageName = "com.shared",
        targets = SHARED_TARGETS,
    )

    val MIXED = Compatibility(
        name = "Mixed",
        packageName = "com.mixed",
        targets = listOf(
            AppTarget(version = "3.0", versionCode = 30),
            AppTarget(version = "2.9", versionCode = 29),
            AppTarget(version = "3.1", versionCode = 31),
        ),
    )

    val TAGGED = Compatibility(
        name = "Tagged",
        packageName = "com.tagged",
        targets = listOf(AppTarget(version = "6.0_arm64", versionCode = 60, minSdk = 21)),
    )
}
"""


def target_lines(text, package):
    lines = text.split("\n")
    start = lines.index(f'        packageName = "{package}",')
    end = lines.index("    )", start)
    return [line.strip() for line in lines[start:end] if "AppTarget(" in line]


def without(text, start, end):
    return text[:text.index(start)] + text[text.index(end):]


# when
inline = bump_prs.add_target(SOURCE, "com.one", "1.1", 11, 26)

# then
assert target_lines(inline, "com.one") == ['AppTarget(version = "1.0", versionCode = 10, minSdk = 24),',
                                           'AppTarget(version = "1.1", versionCode = 11, minSdk = 26),'], inline
assert '        targets = listOf(\n            AppTarget(version = "1.0"' in inline
assert without(inline, "    val ONE", "    val TWO") == without(SOURCE, "    val ONE", "    val TWO")

# when
listed = bump_prs.add_target(SOURCE, "com.two", "2.2", 22, 24)

# then
assert target_lines(listed, "com.two")[-1] == 'AppTarget(version = "2.2", versionCode = 22, minSdk = 24),'
assert len(listed.split("\n")) == len(SOURCE.split("\n")) + 1

# when
newest_first = bump_prs.add_target(SOURCE, "com.newest", "Android V1.80.0", 180, 28)

# then
assert target_lines(newest_first, "com.newest")[0] == 'AppTarget(version = "Android V1.80.0", minSdk = 28),'

refusals = {
    ("com.two", "2.1", 21, 24): "already targeted",
    ("com.two", "2.0.5", 205, 24): "does not follow",
    ("com.shared", "1.0", 1, 24): "not a listOf call",
    ("com.mixed", "3.2", 32, 24): "not sorted",
    ("com.tagged", "6.1", 61, 21): "does not follow",
    ("com.one", "1.1", None, 26): "version code or min SDK unknown",
    ("com.one", "1.1", 11, None): "version code or min SDK unknown",
    ("com.one", '1.1"); evil("', 11, 26): "characters",
    ("com.one", "1.1", "11); evil(", 26): "not a number",
    ("com.missing", "1.0", 1, 24): "0 declarations",
}
for args, reason in refusals.items():
    try:
        bump_prs.add_target(SOURCE, *args)
    except ValueError as error:
        assert reason in str(error), (args, error)
    else:
        raise AssertionError(f"{args} edited the targets")
print("target edits pass")

clean = {"package_ok": True, "version_ok": True, "cert_ok": True, "version": "1.1",
         "patch_check": {"applied": ["A"], "failed": [], "exit": 0}}
ready = {"status": "update", "candidate": "1.1", "readiness": "ready", "verification": {"1.1": [clean]}}
assert bump_prs.bump_apk(ready) is clean
assert bump_prs.bump_apk({**ready, "readiness": "needs work: 1 failed"}) is None
assert bump_prs.bump_apk({**ready, "verification": {"1.1": [{**clean, "cert_ok": False}, clean]}}) is None
assert bump_prs.bump_apk({**ready, "verification": {"1.1": [{**clean, "cert_ok": None}]}})["cert_ok"] is None

subject = "bump(One): add support for `1.1`"
older = "bump(One): add support for `1.0.5`"
assert bump_prs.bump_plan(subject, None, []) == (True, ("create", None))
assert bump_prs.bump_plan(subject, subject, [{"number": 5, "state": "OPEN", "title": subject}]) == (False, None)
assert bump_prs.bump_plan(subject, older, [{"number": 5, "state": "OPEN", "title": older}]) == (True, ("edit", 5))
assert bump_prs.bump_plan(subject, subject, [{"number": 5, "state": "CLOSED", "title": subject}]) == (False, None)
assert bump_prs.bump_plan(subject, subject, []) == (False, ("create", None))
assert bump_prs.bump_plan(subject, older, [{"number": 5, "state": "CLOSED", "title": older}]) == (True, ("create", None))
print("bump plans pass")


def run_git(*args, cwd):
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


prs = []
pr_calls = []


def fake_gh(*args, **kw):
    if args[:2] == ("issue", "list"):
        return json.dumps([{"number": 77, "title": "[App Update]: One 1.1"}])
    if args[:2] == ("pr", "list"):
        return json.dumps(prs)
    pr_calls.append(args[:2])
    if args[:2] == ("pr", "create"):
        bodies.append(args[args.index("--body") + 1])
        prs.append({"number": 9, "state": "OPEN", "title": args[args.index("--title") + 1]})
    return ""


bodies = []


def result(version):
    apk = {**clean, "version": version, "version_code": 11, "min_sdk": 26}
    return [{**ready, "name": "One", "package": "com.one", "candidate": version, "verification": {version: [apk]},
             "current": "1.0", "current_version_codes": [10], "patches": [], "sources": {"play": {"version": version}}}]


with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    (temp / "gitconfig").write_text("[user]\n\tname = test\n\temail = test@example.com\n[init]\n\tdefaultBranch = main\n")
    os.environ.update(GIT_CONFIG_GLOBAL=str(temp / "gitconfig"), GIT_CONFIG_NOSYSTEM="1")
    origin = temp / "origin.git"
    work = temp / "work"
    run_git("init", "-q", "--bare", str(origin), cwd=temp)
    run_git("clone", "-q", str(origin), str(work), cwd=temp)
    (work / bump_prs.COMPAT_PATH).parent.mkdir(parents=True)
    (work / bump_prs.COMPAT_PATH).write_text(SOURCE)
    run_git("add", ".", cwd=work)
    run_git("commit", "-q", "-m", "init", cwd=work)
    run_git("push", "-q", "origin", "main:dev", cwd=work)
    dev_before = run_git("rev-parse", "dev", cwd=origin)
    os.chdir(work)
    bump_prs.gh = fake_gh
    branch = "app-update/com.one"

    # when
    bump_prs.sync_bump_prs(result("1.1"))

    # then
    tip = run_git("rev-parse", branch, cwd=origin)
    assert run_git("rev-parse", f"{branch}^", cwd=origin) == dev_before
    assert run_git("log", "-1", "--format=%B", branch, cwd=origin) == "bump(One): add support for `1.1`\n\nCloses #77"
    assert run_git("log", "-1", "--format=%ae", branch, cwd=origin) == bump_prs.BOT_EMAIL
    assert run_git("diff", "--name-only", "dev", branch, cwd=origin) == bump_prs.COMPAT_PATH
    assert 'AppTarget(version = "1.1", versionCode = 11, minSdk = 26),' in \
        run_git("show", f"{branch}:{bump_prs.COMPAT_PATH}", cwd=origin)
    assert pr_calls == [("pr", "create")]
    assert bodies[0].startswith("### Update\n\nOne `1.0` → `1.1`\n\nPackage `com.one`, version code 10 → 11\n"), bodies[0]
    assert "- [ ] Device check" in bodies[0] and "| Play | 1.1 |" in bodies[0], bodies[0]

    # when
    bump_prs.sync_bump_prs(result("1.1"))

    # then
    assert run_git("rev-parse", branch, cwd=origin) == tip and pr_calls == [("pr", "create")]

    # when
    bump_prs.sync_bump_prs(result("1.2"))

    # then
    assert run_git("rev-parse", f"{branch}^", cwd=origin) == dev_before
    assert run_git("log", "-1", "--format=%B", branch, cwd=origin) == "bump(One): add support for `1.2`"
    assert pr_calls == [("pr", "create"), ("pr", "edit")]
    assert run_git("rev-parse", "dev", cwd=origin) == dev_before
print("bump branches pass")
