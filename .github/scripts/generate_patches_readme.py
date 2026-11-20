#!/usr/bin/env python3
"""python3 generate_patches_readme.py <owner/repo> <branch> [patches-list.json] [README.md] [PATCHES.md]"""

import json
import math
import os
import re
import subprocess
import sys
import urllib.request
from datetime import datetime, timedelta, timezone
from html import escape
from pathlib import Path

import readme_graphics
from versions import APK_DOWNLOAD_WARNING, APK_VERSION_URLS

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "actions" / "triage"))
from fields import field, normalize, parse_fields

INCLUDE_APP_ICONS = True
HOT_WINDOW_DAYS = 30
HOT_WINDOW_MAX_DAYS = 120

if len(sys.argv) < 3:
    print("Usage: generate_patches_readme.py <owner/repo> <branch> [json] [README] [PATCHES]")
    sys.exit(1)

repo_full   = sys.argv[1]
branch      = sys.argv[2]
json_path   = Path(sys.argv[3]) if len(sys.argv) > 3 else Path("patches-list.json")
readme_path = Path(sys.argv[4]) if len(sys.argv) > 4 else Path("README.md")
patches_path = Path(sys.argv[5]) if len(sys.argv) > 5 else Path("PATCHES.md")


if "/" not in repo_full:
    raise ValueError(f"Invalid repo format: {repo_full} (expected owner/repo)")

owner, repo = repo_full.split("/", 1)


with open(json_path, encoding="utf-8") as f:
    data = json.load(f)


by_pkg = {}
universal = {}

for patch in data["patches"]:
    cp = patch.get("compatiblePackages")
    if not cp:
        if patch["name"] not in universal:
            universal[patch["name"]] = patch
        continue
    for pkg_entry in cp:
        pkg  = pkg_entry["packageName"]
        name = pkg_entry.get("name") or pkg
        if pkg not in by_pkg:
            by_pkg[pkg] = {"name": name, "patches": {}, "targets": []}
        for target in pkg_entry.get("targets") or []:
            if target not in by_pkg[pkg]["targets"]:
                by_pkg[pkg]["targets"].append(target)
        if patch["name"] not in by_pkg[pkg]["patches"]:
            by_pkg[pkg]["patches"][patch["name"]] = patch


apps = sorted(by_pkg.items(), key=lambda item: item[1]["name"].lower())


def slug(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def app_heading(app_name, package_name):
    return f"{app_name} ({package_name})" if package_name else app_name


def app_anchor(app_name, package_name):
    heading = app_heading(app_name, package_name)
    return re.sub(r"[^\w\-]+", "", re.sub(r"\s", "-", heading.lower()))


PATCHES_SRC  = Path("patches/src/main/kotlin")
COMPAT_FILE  = PATCHES_SRC / "app/morphe/patches/shared/compat/AppCompatibilities.kt"


def build_source_index():
    if not PATCHES_SRC.is_dir():
        return {}

    compat = {}
    if COMPAT_FILE.is_file():
        text = COMPAT_FILE.read_text(encoding="utf-8")
        for const, body in re.findall(r"val (\w+) = Compatibility\((.*?)\n    \)", text, re.DOTALL):
            pkg = re.search(r'packageName = "([^"]+)"', body)
            if pkg:
                compat[const] = pkg.group(1)

    index = {}
    for path in PATCHES_SRC.rglob("*.kt"):
        if path == COMPAT_FILE:
            continue
        text = path.read_text(encoding="utf-8")
        names = re.findall(r'^\s{4}name = "([^"]+)"', text, re.MULTILINE)
        if not names:
            continue
        consts = re.findall(r"compatibleWith\(\s*AppCompatibilities\.(\w+)", text)
        pkgs = [compat.get(c) for c in consts] or [None]
        for name in names:
            for pkg in pkgs:
                index.setdefault((name, pkg), path.as_posix())
    return index


SOURCES = build_source_index()


def patch_cell(patch, pkg, pid):
    source = SOURCES.get((patch["name"], pkg))
    name = escape(patch["name"], quote=False)
    label = f"[**{name}**]({source})" if source else f"**{name}**"
    return f'<a id="{pid}"></a>{label}'


def clean_description(text):
    return re.sub(r"\s+", " ", text or "").strip()


def markdown_cell(text):
    return escape(text, quote=False).replace("|", "\\|")


OPTIONS_SHOWN = 3


def options_summary(options):
    titles = [opt.get("title") or opt.get("key") for opt in options or []]
    titles = [title for title in titles if title]
    if not titles:
        return ""
    summary = ", ".join(escape(title, quote=False) for title in titles[:OPTIONS_SHOWN])
    if len(titles) > OPTIONS_SHOWN:
        summary += f", +{len(titles) - OPTIONS_SHOWN} more"
    return f"<br><sub>Options: {summary}</sub>"


def patches_table(patches, app_slug, pkg=None):
    rows = ["| Patch | Details |", "|---|---|"]
    for p in sorted(patches, key=lambda x: x["name"]):
        pid = f"{app_slug}-{slug(p['name'])}"
        details = markdown_cell(clean_description(p.get("description"))) + options_summary(p.get("options"))
        rows.append(f"| {patch_cell(p, pkg, pid)} | {details} |")
    return "\n".join(rows)


ANY_VERSION = "Any version"


def version_labels(targets):
    labels = []
    for target in targets:
        version = target.get("version") or ANY_VERSION
        label = f"experimental {version}" if target.get("isExperimental") else version
        if label not in labels:
            labels.append(label)
    if len(labels) > 1:
        labels = [label for label in labels if label != ANY_VERSION]
    return labels


def versions_table(targets, package):
    columns = []
    for t in targets:
        ver = t["version"] or ANY_VERSION
        label = f"🧪&nbsp;{ver}" if t.get("isExperimental") else ver
        if url := APK_VERSION_URLS.get((package, ver)):
            label = f'[{label}]({url} "Download stock APK")'
        if label not in [c[0] for c in columns]:
            columns.append((label, (t.get("description") or "").replace("\n", "<br>")))

    if len(columns) > 1:
        columns = [c for c in columns if c[0] != ANY_VERSION]

    if not columns:
        return ""

    rows = [
        "| " + " | ".join(label for label, _ in columns) + " |",
        "| " + " | ".join(":---:" for _ in columns) + " |",
    ]
    if any(desc for _, desc in columns):
        rows.append("| " + " | ".join(desc for _, desc in columns) + " |")

    return "\n".join(rows)


PLAY_STORE_ICON_URL = "https://publicmetadata.co.uk"
ICON_URLS = {
    "com.unbrokensoftware.launchbox": "https://www.launchbox-app.com/apple-touch-icon-120x120.png",
    "org.quantumbadger.redreaderalpha": "https://raw.githubusercontent.com/QuantumBadger/RedReader/master/src/main/res/mipmap-xxxhdpi/icon_inv.png",
}

FEATURED_PACKAGES = [
    "com.viber.voip",
    "com.bereal.ft",
    "com.imo.android.imoim",
    "com.community.oneroom",
    "ch.protonmail.android",
    "ch.protonvpn.android",
]
HOT_LIMIT = 6
REPORT_WEIGHT = 2
CHANGELOG_SUBJECT = re.compile(r"^(feat|fix|perf|bump)\(([^)]+)\)!?: (.*)")
VERSION_SUPPORT_SUBJECT = re.compile(r"^(?:add )?support (?:for )?`?\d")
def app_icon(package_name, size):
    src = escape(ICON_URLS.get(package_name, f"{PLAY_STORE_ICON_URL}/{package_name}"))
    return f'<img src="{src}" width="{size}" height="{size}" align="top" alt="">'


APP_NOTES_DIR = Path(__file__).resolve().parents[1] / "readme-notes"


def app_notes(pkg):
    path = APP_NOTES_DIR / f"{pkg}.md"
    return f"\n\n{path.read_text(encoding='utf-8').strip()}" if pkg and path.is_file() else ""


def git_lines(*args):
    result = subprocess.run(["git", *args], capture_output=True, text=True)
    return result.stdout.splitlines() if result.returncode == 0 else None


def packages_by_name_key():
    keys = {}
    for pkg, entry in by_pkg.items():
        keys.setdefault(normalize(entry["name"]), []).append(pkg)
    return keys


def packages_for_scope(app, keys):
    key = normalize(app)
    if not key:
        return []
    if key in keys:
        return keys[key]
    prefixed = [name for name in keys if name.startswith(key)]
    return keys[prefixed[0]] if len(prefixed) == 1 else []


def changelog_entries(since, keys):
    if git_lines("rev-parse", "--is-shallow-repository") != ["false"]:
        return None
    subjects = git_lines("log", f"--since={since.isoformat()}", "--no-merges", "--format=%s")
    if subjects is None:
        return None
    entries = {}
    for subject in subjects:
        match = CHANGELOG_SUBJECT.match(subject)
        if not match:
            continue
        kind, scope, text = match.groups()
        app, _, patch = scope.partition(" - ")
        if kind == "feat" and not patch and VERSION_SUPPORT_SUBJECT.match(text):
            kind = "bump"
        for pkg in packages_for_scope(app, keys):
            entry = entries.setdefault(pkg, {"feature_patches": [], "fixed": 0, "versions": 0})
            if kind == "feat":
                entry["feature_patches"].append(patch or None)
            elif kind == "bump":
                entry["versions"] += 1
            else:
                entry["fixed"] += 1
    return entries


def release_note_count(entry):
    return len(entry["feature_patches"]) + entry["fixed"] if entry else 0


def patch_names_before(since):
    start = git_lines("rev-list", "-1", f"--before={since.isoformat()}", "HEAD")
    listing = start and git_lines("show", f"{start[0]}:patches-list.json")
    if not listing:
        return None
    names = {}
    for patch in json.loads("\n".join(listing))["patches"]:
        for pkg_entry in patch.get("compatiblePackages") or []:
            names.setdefault(pkg_entry["packageName"], set()).add(patch["name"])
    return names


PATCH_NAME_DIFF_LINE = re.compile(r'^([+-])\s{4}name = "([^"]+)"')


def commits_touching(names, since):
    commits = []
    for name in sorted(names):
        for commit in git_lines("log", f"--since={since.isoformat()}", f'-S"{name}"', "--format=%H", "--", "patches/src") or []:
            if commit and commit not in commits:
                commits.append(commit)
    return commits


DIFF_FILE_HEADER = re.compile(r"^diff --git a/\S+ b/(\S+)$")


def name_changes_by_file(commit):
    files = {}
    names = None
    for line in git_lines("show", "--format=", "--unified=0", commit, "--", "patches/src") or []:
        if header := DIFF_FILE_HEADER.match(line):
            names = files.setdefault(header.group(1), {"+": set(), "-": set()})
        elif names is not None and (match := PATCH_NAME_DIFF_LINE.match(line)):
            names[match.group(1)].add(match.group(2))
    return files


def renames(pkg, removed, added, since):
    pairs = {}
    for commit in commits_touching(removed, since):
        for file, names in name_changes_by_file(commit).items():
            old = names["-"] & (removed - pairs.keys())
            new = {name for name in names["+"] & (added - set(pairs.values())) if SOURCES.get((name, pkg)) == file}
            if len(old) == 1 and len(new) == 1:
                pairs[old.pop()] = new.pop()
    return pairs


def hot_activity_lines(pkg, entry, names_before, since):
    current = set(by_pkg[pkg]["patches"])
    before = names_before.get(pkg, set())
    added, removed = current - before, before - current
    pairs = renames(pkg, removed, added, since)
    added -= set(pairs.values())
    removed -= pairs.keys()
    entry = entry or {"feature_patches": [], "fixed": 0, "versions": 0}
    counts = [
        (len(added), "new patch", "new patches"),
        (sum(1 for patch in entry["feature_patches"] if patch not in added), "improved", "improved"),
        (entry["fixed"], "fixed", "fixed"),
        (len(removed), "removed", "removed"),
        (entry["versions"], "new version", "new versions"),
    ]
    return [f"{count}&nbsp;{(one if count == 1 else many).replace(' ', '&nbsp;')}" for count, one, many in counts if count]


def next_page(link_header):
    match = re.search(r'<([^>]+)>;\s*rel="next"', link_header or "")
    return match.group(1) if match else None


def fetch_issues_since(since):
    url = (f"https://api.github.com/repos/{owner}/{repo}/issues"
           f"?state=all&per_page=100&since={since.strftime('%Y-%m-%dT%H:%M:%SZ')}")
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "patches-readme"}
    if token := os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN"):
        headers["Authorization"] = f"Bearer {token}"
    issues = []
    try:
        while url:
            with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=20) as response:
                issues.extend(json.load(response))
                url = next_page(response.headers.get("Link"))
    except (OSError, ValueError) as error:
        sys.stderr.write(f"⚠️  Issue reports left out of Hot apps: {error}\n")
        return None
    return issues


def reported_packages(issue, keys):
    reported = normalize(field(parse_fields(issue.get("body") or ""), "App and version"))
    for key in sorted(keys, key=len, reverse=True):
        if reported.startswith(key):
            return keys[key]
    title = issue.get("title") or ""
    return [pkg for pkg, entry in by_pkg.items()
            if re.search(rf"(?<!\w){re.escape(entry['name'])}(?!\w)", title, re.IGNORECASE)][:1]


def issue_reports(since, keys):
    issues = fetch_issues_since(since)
    if issues is None:
        return {}
    counts = {}
    for issue in issues:
        if issue.get("pull_request") or (issue.get("user") or {}).get("type") == "Bot":
            continue
        if datetime.fromisoformat(issue["created_at"].replace("Z", "+00:00")) < since:
            continue
        weight = 1 + (issue.get("reactions") or {}).get("+1", 0)
        for pkg in reported_packages(issue, keys):
            counts[pkg] = counts.get(pkg, 0) + weight
    return counts


def hot_apps():
    keys = packages_by_name_key()
    days, usable = HOT_WINDOW_DAYS, None
    while True:
        since = datetime.now(timezone.utc) - timedelta(days=days)
        names_before = patch_names_before(since)
        if names_before is None:
            sys.stderr.write(f"⚠️  No patches-list.json from before {since:%Y-%m-%d} to compare with, Hot apps stop at the {days}-day window\n")
            break
        found = hot_apps_since(since, days, keys)
        if found is None:
            break
        usable = found, names_before, since
        if len(found[0]) >= HOT_LIMIT or days >= HOT_WINDOW_MAX_DAYS:
            break
        days = min(days * 2, HOT_WINDOW_MAX_DAYS)
    if not usable:
        return []
    (ranked, changes), names_before, since = usable
    return [(pkg, hot_activity_lines(pkg, changes.get(pkg), names_before, since)) for pkg in ranked]


def hot_apps_since(since, days, keys):
    changes = changelog_entries(since, keys)
    if changes is None:
        sys.stderr.write("⚠️  Full Git history with recent commits needed, Hot apps left out\n")
        return None
    reports = issue_reports(since, keys)

    def score(pkg):
        activity = release_note_count(changes.get(pkg)) + REPORT_WEIGHT * reports.get(pkg, 0)
        return activity + math.log2(1 + len(by_pkg[pkg]["patches"])) if activity else 0

    ranked = sorted((pkg for pkg in by_pkg if pkg not in FEATURED_PACKAGES and score(pkg) > 0),
                    key=lambda pkg: (-score(pkg), by_pkg[pkg]["name"].lower()))
    for pkg in ranked[:HOT_LIMIT]:
        sys.stderr.write(f"Hot ({days} days): {by_pkg[pkg]['name']} {release_note_count(changes.get(pkg))} changes, {reports.get(pkg, 0)} reports\n")
    return ranked[:HOT_LIMIT], changes


GRID_COLUMNS = 3
GRID_CELL_WIDTH = 100


def patch_count(n):
    return f"{n}&nbsp;{'patch' if n == 1 else 'patches'}"


def grid_cell(pkg, caption_lines=None):
    label = by_pkg[pkg]["name"]
    icon = f"{app_icon(pkg, 40)}<br>" if INCLUDE_APP_ICONS else ""
    caption = "<br>".join(caption_lines or [patch_count(len(by_pkg[pkg]["patches"]))])
    return (f'<td align="center" valign="top" width="{GRID_CELL_WIDTH}"><a href="PATCHES.md#{app_anchor(label, pkg)}">{icon}{label}</a>'
            f"<br><sub>{caption}</sub></td>")


def app_grid(cells):
    rows = [cells[i:i + GRID_COLUMNS] for i in range(0, len(cells), GRID_COLUMNS)]
    body = "\n".join("<tr>\n" + "\n".join(row) + "\n</tr>" for row in rows)
    return f'<table align="center">\n{body}\n</table>\n'


INDEX_NAME_CHARS = 20
INDEX_VERSION_CHARS = 16
INDEX_PACKAGE_CHARS = 24
ELLIPSIS = "…"


def shortened(text, limit):
    if len(text) <= limit:
        return text
    return text[:limit - len(ELLIPSIS)] + ELLIPSIS


def shortened_name(name, limit):
    if len(name) <= limit:
        return name
    head = name[:limit - len(ELLIPSIS)]
    if " " in head and name[len(head)] != " ":
        head = head.rpartition(" ")[0]
    return head.rstrip(" -:&") + ELLIPSIS


def hover_title(full, shown):
    return f' "{escape(full)}"' if shown != full else ""


def version_cell(labels):
    if not labels:
        return "-"
    shown = []
    length = 0
    for label in labels:
        added = len(label) + (len(", ") if shown else 0)
        if shown and length + added > INDEX_VERSION_CHARS:
            break
        shown.append(shortened(label, INDEX_VERSION_CHARS))
        length += added
    cell = ", ".join(f"`{escape(label, quote=False)}`" for label in shown)
    hidden = len(labels) - len(shown)
    return f"{cell}&nbsp;+{hidden}" if hidden else cell


def index_row(index, app_name, package_name, count, labels):
    name = shortened_name(app_name, INDEX_NAME_CHARS)
    app_cell = f"[**{escape(name, quote=False)}**](PATCHES.md#{app_anchor(app_name, package_name)}{hover_title(app_name, name)})"
    if INCLUDE_APP_ICONS and package_name:
        app_cell = f"{app_icon(package_name, 24)} {app_cell}"
    package = shortened(package_name, INDEX_PACKAGE_CHARS)
    package_cell = (
        f"[`{escape(package, quote=False)}`](https://play.google.com/store/apps/details?id={package_name}{hover_title(package_name, package)})"
        if package_name
        else "-"
    )
    return f"| {index} | {app_cell} | {count} | {version_cell(labels)} | {package_cell} |"


def featured_packages():
    for pkg in FEATURED_PACKAGES:
        if pkg not in by_pkg:
            sys.stderr.write(f"⚠️  Featured app {pkg} is not in {json_path}, left out of Featured apps\n")
    return [pkg for pkg in FEATURED_PACKAGES if pkg in by_pkg]


def build_readme_section():
    lines = [
        f"> **[v{ver}](https://github.com/{owner}/{repo}/releases/tag/v{ver})**"
        f"&nbsp;&nbsp;&middot;&nbsp;&nbsp;`{branch}`&nbsp;&nbsp;&middot;&nbsp;&nbsp;"
        f"**{total} patches**"
        f"&nbsp;&nbsp;&middot;&nbsp;&nbsp;[Full details](PATCHES.md)"
    ]

    lines.append("")
    if featured := featured_packages():
        lines.append("### Featured apps\n")
        lines.append(app_grid([grid_cell(pkg) for pkg in featured]))
    if hot := hot_apps():
        lines.append("### Hot apps\n")
        lines.append(app_grid([grid_cell(pkg, caption_lines) for pkg, caption_lines in hot]))
    lines.append("### All apps\n")
    lines.append(f"<details>\n<summary>Tap to reveal all {len(apps)} apps</summary>\n")
    lines.append("| # | App | Patches | Version | Package |")
    lines.append("|---|---|---|---|---|")
    for index, (pkg, entry) in enumerate(apps, start=1):
        lines.append(index_row(index, entry["name"], pkg, len(entry["patches"]), version_labels(entry["targets"])))
    if universal:
        lines.append(index_row(len(apps) + 1, "Universal", "", len(universal), []))
    lines.append("\n</details>\n")
    return "\n".join(lines)


INDEX_START = "<!-- INDEX_START -->"
INDEX_END = "<!-- INDEX_END -->"
GRAPHICS_PATH = ".github/readme"
ASSETS_REF = "origin/assets"
INDEX_BUTTON_WIDTH = 155
INDEX_BUTTONS = [
    ("install", "Install", "Add the source", "installing", "#-installing"),
    ("patches", "Patches", None, "full-list", "#-patches-list"),
    ("request", "Request", "Vote or ask", "requests", "#-requests"),
    ("build", "Build", "From source", "building", "#-building"),
    ("disclaimer", "Disclaimer", "Read first", "disclaimer", "#-disclaimer"),
    ("license", "License", "GPLv3 and notices", "license", "#-license"),
]
TAPE_ITEMS = 14
TAPE_VERSION = re.compile(r"^(?:add )?support (?:for )?(.+)$")
TAPE_VERSION_TOKEN = re.compile(r"`([^`]+)`|(\w[\w.+-]*\d[\w.+-]*)")
TAPE_THEMES = ("light", "dark")


def assets_icon(name):
    result = subprocess.run(["git", "show", f"{ASSETS_REF}:aqua/icons/{name}.png"], capture_output=True)
    return result.stdout if result.returncode == 0 and result.stdout else None


def tape_items():
    if git_lines("rev-parse", "--is-shallow-repository") != ["false"]:
        sys.stderr.write("⚠️  Full Git history needed, release tape left out\n")
        return []
    since = datetime.now(timezone.utc) - timedelta(days=HOT_WINDOW_DAYS)
    names_before = patch_names_before(since)
    if names_before is None:
        sys.stderr.write(f"⚠️  No patches-list.json from before {since:%Y-%m-%d}, release tape left out\n")
        return []
    keys = packages_by_name_key()
    items = []
    for subject in git_lines("log", f"--since={since.isoformat()}", "--no-merges", "--format=%s") or []:
        match = CHANGELOG_SUBJECT.match(subject)
        if not match:
            continue
        kind, scope, text = match.groups()
        app, _, patch = scope.partition(" - ")
        pkgs = packages_for_scope(app, keys)
        if not pkgs:
            continue
        pkg = pkgs[0]
        if not patch and (kind == "bump" or (kind == "feat" and VERSION_SUPPORT_SUBJECT.match(text))):
            version = TAPE_VERSION.match(text)
            tokens = [quoted or bare for quoted, bare in TAPE_VERSION_TOKEN.findall(version.group(1))] if version else []
            if not tokens:
                continue
            item = ("Version", by_pkg[pkg]["name"], " and ".join(tokens))
        elif patch:
            if kind == "feat" and patch not in names_before.get(pkg, set()):
                label = "New"
            elif kind == "fix":
                label = "Fixed"
            else:
                label = "Improved"
            item = (label, by_pkg[pkg]["name"], patch)
        else:
            continue
        if item in items:
            continue
        if not readme_graphics.covered(" ".join(item)):
            sys.stderr.write(f"⚠️  Release tape left out {' '.join(item)!r}: glyphs missing from the font\n")
            continue
        items.append(item)
        if len(items) == TAPE_ITEMS:
            break
    return items


def build_index():
    icons = {icon: assets_icon(icon) for _, _, _, icon, _ in INDEX_BUTTONS}
    missing = sorted(icon for icon, png in icons.items() if png is None)
    if missing:
        sys.stderr.write(f"⚠️  Icons {missing} not on {ASSETS_REF}, index buttons and release tape left as they are\n")
        return None
    buttons = [(key, label, caption or f"{total} for {len(apps)} apps", icon, anchor) for key, label, caption, icon, anchor in INDEX_BUTTONS]
    font_text = "".join(label + caption for _, label, caption, _, _ in buttons)
    try:
        files = {f"{key}.svg": readme_graphics.badge_svg(label, caption, icons[icon], font_text) for key, label, caption, icon, _ in buttons}
    except ValueError as error:
        sys.stderr.write(f"⚠️  {error}. Nothing written.\n")
        sys.exit(1)
    links = [f'<a href="{anchor}"><img src="{GRAPHICS_PATH}/{key}.svg" width="{INDEX_BUTTON_WIDTH}" alt="{label}"></a>' for key, label, _, _, anchor in buttons]
    lines = ['<p align="center">', "<br>\n".join("&nbsp;".join(links[row:row + 2]) for row in range(0, len(links), 2)), "</p>"]
    tape = tape_items()
    for theme in TAPE_THEMES:
        files[f"tape-{theme}.svg"] = readme_graphics.tape_svg(tape, theme) if tape else None
    if tape:
        tape_url = f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{GRAPHICS_PATH}/tape"
        picture = (f'<picture><source media="(prefers-color-scheme: dark)" srcset="{tape_url}-dark.svg">'
                   f'<img src="{tape_url}-light.svg" width="{readme_graphics.TAPE_WIDTH}" alt="Latest changes"></picture>')
        lines += ["", f'<p align="center"><a href="https://github.com/{owner}/{repo}/releases">{picture}</a></p>']
    return "\n".join(lines), files


def build_patches_doc():
    sections = [
        "# Patches",
        "",
        f"> Generated from `patches-list.json` for **v{ver}** (`{branch}`)"
        f"&nbsp;&nbsp;&middot;&nbsp;&nbsp;**{total} patches** across **{len(apps)} apps**"
        f"&nbsp;&nbsp;&middot;&nbsp;&nbsp;back to [README](README.md)",
        "",
        "---",
        "",
    ]

    for pkg, entry in apps:
        label = entry["name"]
        sections.append(f"## {app_heading(label, pkg)}")
        sections.append("")
        if INCLUDE_APP_ICONS:
            sections.append(app_icon(pkg, 48))
            sections.append("")
        vtbl = versions_table(entry["targets"], pkg)
        if vtbl:
            sections.append(f"**Supported versions:**\n\n{vtbl}\n")
        if any((pkg, target.get("version")) in APK_VERSION_URLS for target in entry["targets"]):
            sections.append(APK_DOWNLOAD_WARNING + "\n")
        sections.append(patches_table(list(entry["patches"].values()), slug(label), pkg) + app_notes(pkg))
        sections.append("")
        sections.append("---")
        sections.append("")

    if universal:
        sections.append("## Universal")
        sections.append("")
        sections.append(patches_table(list(universal.values()), "universal"))
        sections.append("")

    return "\n".join(sections).rstrip() + "\n"


raw_ver = data["version"]
ver   = raw_ver.lstrip("v")
total = len(data["patches"])

readme = readme_path.read_text(encoding="utf-8")

START_PATTERN = r"<!-- PATCHES_START\s*-->"
END_MARKER    = "<!-- PATCHES_END -->"

start_matches = list(re.finditer(START_PATTERN, readme))
end_positions = [match.start() for match in re.finditer(re.escape(END_MARKER), readme)]

if len(start_matches) != 1 or len(end_positions) != 1 or end_positions[0] < start_matches[0].end():
    sys.stderr.write(
        f"⚠️  {readme_path} needs exactly one <!-- PATCHES_START --> followed by one {END_MARKER}, "
        f"found {len(start_matches)} start and {len(end_positions)} end markers. Nothing written.\n"
    )
    sys.exit(1)

actual_start = start_matches[0].group(0)

index_starts, index_ends = readme.count(INDEX_START), readme.count(INDEX_END)
has_index = (index_starts, index_ends) != (0, 0)
if has_index and (index_starts != 1 or index_ends != 1 or readme.index(INDEX_END) < readme.index(INDEX_START)):
    sys.stderr.write(
        f"⚠️  {readme_path} needs exactly one {INDEX_START} followed by one {INDEX_END}, "
        f"found {index_starts} start and {index_ends} end markers. Nothing written.\n"
    )
    sys.exit(1)

generated  = build_readme_section()
index = build_index() if has_index else None

readme = readme.replace("https://morphe.software/add-source?github=xyz-user/xyz-patches", f"https://morphe.software/add-source?github={repo_full}")
readme = readme.replace("https://github.com/xyz-user/xyz-patches", f"https://github.com/{repo_full}")

new_readme = re.sub(
    rf"{START_PATTERN}.*?{re.escape(END_MARKER)}",
    f"{actual_start}\n{generated}\n{END_MARKER}",
    readme,
    flags=re.DOTALL,
)
if index:
    index_block, graphics = index
    start, end = new_readme.index(INDEX_START) + len(INDEX_START), new_readme.index(INDEX_END)
    new_readme = f"{new_readme[:start]}\n{index_block}\n{new_readme[end:]}"
    graphics_dir = readme_path.parent / GRAPHICS_PATH
    graphics_dir.mkdir(parents=True, exist_ok=True)
    for name, content in graphics.items():
        if content is None:
            (graphics_dir / name).unlink(missing_ok=True)
        else:
            (graphics_dir / name).write_text(content, encoding="utf-8")
readme_path.write_text(new_readme, encoding="utf-8")
patches_path.write_text(build_patches_doc(), encoding="utf-8")
print(f"✅ Injected patches section into {readme_path} and wrote {patches_path} (v{ver}, branch={branch}, {total} patches, {len(apps)} apps)")
