#!/usr/bin/env python3

import json
import os
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timedelta, timezone
from html import unescape
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/generate_patches_readme.py"
ICON_SWITCH = re.compile(r"^INCLUDE_APP_ICONS = (True|False)$", re.MULTILINE)
PLAY_STORE_ICON_HOST = "publicmetadata.co.uk"
OVERRIDE_ICON_PACKAGE = "com.unbrokensoftware.launchbox"
OVERRIDE_ICON_HOST = "launchbox-app.com"
RAW_OVERRIDE_ICON_HOST = "raw.githubusercontent.com/QuantumBadger"
YUKA_HEADING_ANCHOR = "yuka-ioyukaandroid"
UNIVERSAL_HEADING_ANCHOR = "universal"
AMPERSAND_HEADING_ANCHOR = "transparent-clock--weather-comdroid27transparentclockweather"
PATCHES_DOC_LINK = re.compile(r"PATCHES\.md#([^)\" ]+)")
INDEX_ROW = re.compile(r"^\| \d+ \| ")
INDEX_NAME = re.compile(r"\[\*\*(.+?)\*\*\]")
INDEX_CODE = re.compile(r"`([^`]+)`")
MARKDOWN_IN_HEADING = re.compile(r"[*`\[\]<>\\~]|&(#?\w+);|(^|\W)_|_(\W|$)")
START_MARKER = "<!-- PATCHES_START -->"
END_MARKER = "<!-- PATCHES_END -->"
FEATURED_PACKAGE = "com.viber.voip"
RUN_WITH_SWITCH = (
    "import re, sys, os\n"
    "script, value = sys.argv[1], sys.argv[2]\n"
    "source, count = re.subn(r'^INCLUDE_APP_ICONS = (True|False)$', f'INCLUDE_APP_ICONS = {value}', open(script, encoding='utf-8').read(), flags=re.MULTILINE)\n"
    "assert count == 1\n"
    "sys.argv = [script, *sys.argv[3:]]\n"
    "sys.path.insert(0, os.path.dirname(script))\n"
    "exec(compile(source, script, 'exec'), {'__file__': script, '__name__': '__main__'})\n"
)


def heading_anchors(patches_doc):
    return {
        re.sub(r"[^\w\-]+", "", re.sub(r"\s", "-", line[3:].lower()))
        for line in patches_doc.splitlines()
        if line.startswith("## ")
    }


def run_generator(directory, readme_text, catalog=ROOT / "patches-list.json", icons=True, cwd=None):
    readme = directory / "README.md"
    patches_doc = directory / "PATCHES.md"
    readme.write_text(readme_text)
    command = [sys.executable, "-c", RUN_WITH_SWITCH, str(SCRIPT), str(icons)]
    result = subprocess.run(
        [*command, "hxreborn/morphe-patches", "dev", str(catalog), str(readme), str(patches_doc)],
        cwd=cwd or directory, capture_output=True, text=True,
    )
    return result, readme.read_text(), patches_doc.read_text() if patches_doc.exists() else None


def generate(directory, icons):
    result, readme, patches_doc = run_generator(directory, f"{START_MARKER}\n{END_MARKER}\n", icons=icons, cwd=ROOT)
    assert result.returncode == 0, result.stderr
    return readme, patches_doc


def load_generator(directory):
    readme = directory / "README.md"
    readme.write_text(f"{START_MARKER}\n{END_MARKER}\n")
    sys.path.insert(0, str(SCRIPT.parent))
    namespace = {"__name__": "__main__", "__file__": str(SCRIPT)}
    argv, cwd = sys.argv, Path.cwd()
    sys.argv = [str(SCRIPT), "hxreborn/morphe-patches", "dev", str(ROOT / "patches-list.json"), str(readme), str(directory / "PATCHES.md")]
    os.chdir(directory)
    try:
        exec(compile(SCRIPT.read_text(), str(SCRIPT), "exec"), namespace)
    finally:
        sys.argv = argv
        os.chdir(cwd)
    return namespace


def fake_git(log=(), baseline=None, diffs=None, baseline_days=None):
    def has_baseline(before):
        if baseline is None:
            return False
        since = datetime.fromisoformat(before.removeprefix("--before="))
        return baseline_days is None or since >= datetime.now(timezone.utc) - timedelta(days=baseline_days + 1)

    def git_lines(*args):
        if args[:2] == ("rev-parse", "--is-shallow-repository"):
            return ["false"]
        if args[0] == "log" and "--no-merges" in args:
            return list(log)
        if args[0] == "log":
            return list(diffs or {})
        if args[0] == "rev-list":
            return ["baseline"] if has_baseline(args[2]) else []
        if args[0] == "show" and args[1].startswith("baseline:"):
            return json.dumps(baseline).splitlines() if baseline is not None else None
        if args[0] == "show":
            return (diffs or {}).get(args[3])
        return None
    return git_lines


assert ICON_SWITCH.search(SCRIPT.read_text())
data = json.loads((ROOT / "patches-list.json").read_text())
app_count = len({pkg["packageName"] for patch in data["patches"] for pkg in patch.get("compatiblePackages") or []})

with tempfile.TemporaryDirectory() as temp:
    readme, patches_doc = generate(Path(temp), icons=False)
    assert PLAY_STORE_ICON_HOST not in readme and PLAY_STORE_ICON_HOST not in patches_doc
    assert OVERRIDE_ICON_HOST not in readme and OVERRIDE_ICON_HOST not in patches_doc
    assert "<img" not in patches_doc
    generated = readme.split("<!-- PATCHES_START -->")[1].split("<!-- PATCHES_END -->")[0]
    assert not [line for line in generated.splitlines() if "<img" in line]
    anchors = heading_anchors(patches_doc)
    links = PATCHES_DOC_LINK.findall(readme)
    assert UNIVERSAL_HEADING_ANCHOR in anchors
    assert len(anchors - {UNIVERSAL_HEADING_ANCHOR}) == app_count, (len(anchors), app_count)
    assert links and all(link in anchors for link in links), [link for link in links if link not in anchors]
    assert AMPERSAND_HEADING_ANCHOR in anchors and YUKA_HEADING_ANCHOR in anchors

with tempfile.TemporaryDirectory() as temp:
    readme, patches_doc = generate(Path(temp), icons=True)
    assert patches_doc.count(f'src="https://{PLAY_STORE_ICON_HOST}/') + patches_doc.count(OVERRIDE_ICON_HOST) + patches_doc.count(RAW_OVERRIDE_ICON_HOST) == app_count
    assert OVERRIDE_ICON_PACKAGE in readme and OVERRIDE_ICON_HOST in patches_doc
    assert PLAY_STORE_ICON_HOST in readme
    rows = [line.split(" | ") for line in readme.splitlines() if INDEX_ROW.match(line)]
    assert len(rows) == app_count + 1, len(rows)
    names = [unescape(INDEX_NAME.search(row[1]).group(1)) for row in rows]
    packages = [match.group(1) for row in rows if (match := INDEX_CODE.search(row[4]))]
    versions = [", ".join(INDEX_CODE.findall(row[3])) for row in rows]
    assert max(map(len, names)) <= 20 and max(map(len, packages)) <= 24, (max(names, key=len), max(packages, key=len))
    assert all(len(version) <= 16 or ", " not in version for version in versions), max(versions, key=len)
    assert '[**Audiobook Unlimited…**](PATCHES.md#' in readme and ' "Audiobook Unlimited for Kindle")' in readme
    assert ' "ringtonemaker.musiccutter.customringtones.freeringtonemaker")' in readme

for name, markers in {
    "reversed": f"{END_MARKER}\nold\n{START_MARKER}\n",
    "duplicated": f"{START_MARKER}\nold\n{END_MARKER}\n" * 2,
    "start only": f"{START_MARKER}\nold\n",
    "end only": f"old\n{END_MARKER}\n",
}.items():
    with tempfile.TemporaryDirectory() as temp:
        result, readme, patches_doc = run_generator(Path(temp), markers)
        assert result.returncode == 1, (name, result.returncode)
        assert readme == markers and patches_doc is None, name

with tempfile.TemporaryDirectory() as temp:
    original = (ROOT / "README.md").read_text()
    result, readme, patches_doc = run_generator(Path(temp), original)
    assert result.returncode == 0, result.stderr
    assert original.count(START_MARKER) == 1 and original.count(END_MARKER) == 1
    assert readme.split(START_MARKER)[0] == original.split(START_MARKER)[0]
    assert readme.split(END_MARKER)[1] == original.split(END_MARKER)[1]

INDEX_START = "<!-- INDEX_START -->"
INDEX_END = "<!-- INDEX_END -->"
PATCHES_BLOCK = f"{START_MARKER}\n{END_MARKER}\n"

for name, markers in {
    "reversed": f"{INDEX_END}\n{INDEX_START}\n",
    "duplicated": f"{INDEX_START}\n{INDEX_END}\n" * 2,
    "start only": f"{INDEX_START}\n",
    "end only": f"{INDEX_END}\n",
}.items():
    with tempfile.TemporaryDirectory() as temp:
        result, readme, patches_doc = run_generator(Path(temp), markers + PATCHES_BLOCK, cwd=ROOT)
        assert result.returncode == 1, (name, result.returncode)
        assert readme == markers + PATCHES_BLOCK and patches_doc is None, name
        assert not (Path(temp) / ".github").exists(), name

with tempfile.TemporaryDirectory() as temp:
    original = (ROOT / "README.md").read_text()
    result, readme, patches_doc = run_generator(Path(temp), original, cwd=ROOT)
    assert result.returncode == 0, result.stderr
    graphics = Path(temp) / ".github/readme"
    for button in ("install", "patches", "request", "build", "disclaimer", "license"):
        assert (graphics / f"{button}.svg").read_bytes() == (ROOT / ".github/readme" / f"{button}.svg").read_bytes(), button
        assert f'src=".github/readme/{button}.svg" width="42%"' in readme, button
    tape_files = {(graphics / f"tape-{theme}.svg").is_file() for theme in ("light", "dark")}
    assert tape_files == {"/.github/readme/tape-light.svg" in readme}
    assert readme.split(INDEX_START)[0] == original.split(INDEX_START)[0]

with tempfile.TemporaryDirectory() as temp:
    stale_index = f"{INDEX_START}\nold index\n{INDEX_END}\n"
    result, readme, patches_doc = run_generator(Path(temp), stale_index + PATCHES_BLOCK)
    assert result.returncode == 0, result.stderr
    assert readme.startswith(stale_index) and "index buttons and release tape left as they are" in result.stderr
    assert not (Path(temp) / ".github").exists()

sys.path.insert(0, str(SCRIPT.parent))
import readme_graphics

icon = (ROOT / ".github/readme/install.svg").read_bytes()
for label, caption in (("Install", "A caption far too long for the badge"), ("Install", "Add the source ✓")):
    try:
        readme_graphics.badge_svg(label, caption, icon, label + caption)
    except ValueError:
        pass
    else:
        raise AssertionError(f"badge_svg accepted {caption!r}")

with tempfile.TemporaryDirectory() as temp:
    catalog = Path(temp) / "patches-list.json"
    catalog.write_text(json.dumps(data | {"patches": [
        patch for patch in data["patches"]
        if FEATURED_PACKAGE not in {pkg["packageName"] for pkg in patch.get("compatiblePackages") or []}
    ]}))
    result, readme, patches_doc = run_generator(Path(temp), f"{START_MARKER}\n{END_MARKER}\n", catalog=catalog)
    assert result.returncode == 0, result.stderr
    assert "### Featured apps" in readme and FEATURED_PACKAGE not in readme and patches_doc
    assert FEATURED_PACKAGE in result.stderr

with tempfile.TemporaryDirectory() as temp:
    generator = load_generator(Path(temp))
    weather = "com.handmark.expressweather"
    weather_patches = set(generator["by_pkg"][weather]["patches"])
    baseline = {"patches": [{"name": name, "compatiblePackages": [{"packageName": weather}]} for name in weather_patches]}

    since = datetime.now(timezone.utc)

    generator["git_lines"] = fake_git()
    assert generator["changelog_entries"](since, {}) == {}
    windows = []
    issue_reports = generator["issue_reports"]
    generator["issue_reports"] = lambda since, keys: windows.append(since) or {}
    generator["git_lines"] = fake_git(baseline=baseline)
    assert generator["hot_apps"]() == [] and len(windows) == 6

    generator["git_lines"] = fake_git(log=["fix(1Weather): fix the radar crash"])
    assert generator["patch_names_before"](since) is None
    assert generator["hot_apps"]() == []

    generator["git_lines"] = fake_git(log=["fix(1Weather): fix the radar crash"], baseline=baseline)
    hot = dict(generator["hot_apps"]())
    assert hot[weather] == ["1&nbsp;fixed"], hot

    generator["git_lines"] = fake_git(log=["fix(1Weather): fix the radar crash"], baseline=baseline, baseline_days=30)
    hot = dict(generator["hot_apps"]())
    assert hot[weather] == ["1&nbsp;fixed"], hot

    generator["fetch_issues_since"] = lambda since: [{"title": "1Weather crash", "user": None, "created_at": since.isoformat(), "reactions": {}}]
    reports = issue_reports(since - timedelta(days=1), generator["packages_by_name_key"]())
    assert reports == {weather: 1}, reports

    readera = "org.readera"
    generator["SOURCES"] = {("Read footnotes aloud", readera): "A.kt", ("Omit words and symbols", readera): "C.kt", ("Unrelated new", readera): "B.kt"}
    split_rename = {"one": [
        "diff --git a/A.kt b/A.kt", "@@ -1 +1 @@", '-    name = "Footnote speech",', '+    name = "Read footnotes aloud",',
        "diff --git a/B.kt b/B.kt", "@@ -1 +0,0 @@", '-    name = "Text-to-speech filter",', '-    name = "onCreate",',
        "diff --git a/C.kt b/C.kt", "@@ -0,0 +1 @@", '+    name = "Omit words and symbols",', '+    name = "onResume",',
    ]}
    generator["git_lines"] = fake_git(diffs=split_rename)
    assert generator["renames"](readera, {"Footnote speech", "Text-to-speech filter"}, {"Read footnotes aloud", "Omit words and symbols"}, since) == {
        "Footnote speech": "Read footnotes aloud"}
    assert generator["renames"]("com.handmark.expressweather", {"Footnote speech"}, {"Read footnotes aloud"}, since) == {}
    unrelated_files = {"one": [
        "diff --git a/A.kt b/A.kt", "@@ -1 +0,0 @@", '-    name = "Old",',
        "diff --git a/B.kt b/B.kt", "@@ -0,0 +1 @@", '+    name = "Unrelated new",',
    ]}
    generator["git_lines"] = fake_git(diffs=unrelated_files)
    assert generator["renames"](readera, {"Old"}, {"Unrelated new"}, since) == {}

with tempfile.TemporaryDirectory() as temp:
    _, patches_doc = generate(Path(temp), icons=False)
    headings = [line[3:] for line in patches_doc.splitlines() if line.startswith("## ")]
    assert not [heading for heading in headings if MARKDOWN_IN_HEADING.search(heading)]

print("PASS README and patches page without icons, with icons, index anchors, markers, featured apps and Hot apps")
