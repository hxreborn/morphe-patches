#!/usr/bin/env python3
"""python3 app_request.py <issue-number> <issue-body-file> <requests.json> <patches-list.json> [bundles.json]"""

import json
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from fields import field, normalize, parse_fields

MARKER = "<!-- app-request-bot -->"
URL = re.compile(r"https?://\S+")
URL_PACKAGE = re.compile(r"[?&](?:id|p)=([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+)")
PACKAGE = re.compile(r"\b[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+\b")
DOMAIN_ENDINGS = {"com", "net", "org", "io", "ph", "me", "app", "co", "in", "tv", "apk", "xyz", "to"}
NAME_NOISE = {"app", "android", "apk", "for", "the"}
MAX_LISTED = 5
QUIET_DAYS = 14


def is_package(text):
    parts = text.split(".")
    return bool(PACKAGE.fullmatch(text)) and not (len(parts) == 2 and parts[1].lower() in DOMAIN_ENDINGS)


def find_package(body):
    fields = parse_fields(body)
    declared = field(fields, "Package name", "Package").strip().strip("`").strip()
    if is_package(declared):
        return declared
    text = "\n".join(fields.values()) or body
    linked = URL_PACKAGE.search(text)
    if linked:
        return linked.group(1)
    return next((m.group(0) for m in PACKAGE.finditer(URL.sub(" ", text)) if is_package(m.group(0))), "")


def app_name(body, title=""):
    raw = field(parse_fields(body), "App") or re.sub(r"^\[App Request\]:\s*", "", title, flags=re.I)
    raw = re.split(r"[,(:]| - ", URL.sub(" ", raw), maxsplit=1)[0]
    return "".join(w for w in re.findall(r"[a-z0-9]+", raw.lower()) if w not in NAME_NOISE)


def supported_apps(patches_list):
    return {
        package["packageName"].lower(): package["name"]
        for patch in patches_list["patches"]
        for package in patch.get("compatiblePackages") or []
    }


def discussed_recently(issue, now):
    cutoff = now - timedelta(days=QUIET_DAYS)
    return any(
        (c.get("author") or {}).get("login") != "github-actions"
        and datetime.fromisoformat(c["createdAt"].replace("Z", "+00:00")) > cutoff
        for c in issue.get("comments") or []
    )


def link(issue):
    return f"#{issue['number']}"


def decide(number, body, requests, patches_list, registry=None, title="", now=None):
    package = find_package(body)
    name = app_name(body, title)
    supported = supported_apps(patches_list)
    earlier = sorted((r for r in requests if r["number"] < number), key=lambda r: r["number"])
    same_package = [r for r in earlier if package and find_package(r["body"] or "").lower() == package.lower()]
    open_same = [r for r in same_package if r["state"] == "OPEN"]
    verdict, reason, duplicate_of, labels, lines = "pass", "", 0, [], []

    if package and package.lower() in supported:
        verdict, reason = "close", "completed"
        lines.append(
            f"`{package}` is already supported here as **{supported[package.lower()]}**. Update the "
            "bundle in Morphe Manager and patch it. If you need something the current patches "
            "don't do, open a feature request."
        )
    elif open_same:
        canonical = open_same[0]
        verdict, reason, duplicate_of = "close", "duplicate", canonical["number"]
        lines.append(
            f"{link(canonical)} already requests `{package}`, so this one is closed as a duplicate. "
            f"Upvote {link(canonical)} and add anything new there."
        )
    elif same_package:
        prior = same_package[-1]
        verdict, labels = "flag", ["duplicate"]
        lines.append(
            f"{link(prior)} asked for `{package}` before and was closed. Leaving this open for "
            "review. If something changed since then, say what."
        )
    elif not package:
        verdict, labels = "flag", ["needs info"]
        lines.append(
            "**I can't find a package name.** Put it in the Package name field, for example "
            "`com.duolingo`. It's the `id=` part of the Play Store link."
        )

    this = next((r for r in requests if r["number"] == number), {})
    if verdict == "close" and discussed_recently(this, now or datetime.now(timezone.utc)):
        verdict, reason, duplicate_of, lines = "pass", "", 0, []

    if verdict != "close" and name:
        lookalikes = [r for r in earlier if r["state"] == "OPEN" and r not in same_package
                      and app_name(r["body"] or "", r.get("title", "")) == name]
        lookalikes += [{"number": 0, "name": n} for p, n in supported.items()
                       if normalize(n) == name and p != (package or "").lower()]
        if lookalikes:
            verdict = "flag"
            if "duplicate" not in labels:
                labels.append("duplicate")
            refs = ", ".join(f"**{r['name']}** (supported)" if not r["number"] else link(r) for r in lookalikes)
            lines.append(
                f"This looks like the same app as {refs}, but the package name differs or is "
                "missing. Leaving it open for review."
            )

    bundles = []
    if package and verdict != "close" and registry:
        bundles = sorted(
            (b for b in registry.get("bundles", []) if package in (b.get("targetApps") or [])),
            key=lambda b: -(b.get("stars") or 0),
        )
        if bundles:
            host = {"github": "https://github.com", "gitlab": "https://gitlab.com"}
            rows = [
                f"- [{b.get('name') or b['repo']}]({host.get(b.get('source'), host['github'])}/{b['repo']})"
                + (f" ({b['stars']} stars)" if b.get("stars") else "")
                for b in bundles[:MAX_LISTED]
            ]
            if len(bundles) > MAX_LISTED:
                rows.append(f"- and {len(bundles) - MAX_LISTED} more")
            lines.append(
                f"`{package}` already has patches elsewhere:\n\n" + "\n".join(rows) + "\n\n"
                "Add that bundle as a source in Morphe Manager. If you need something those "
                "patches don't cover, say what."
            )

    if verdict != "pass":
        lines.append("If this is wrong, add the `bad-triage` label or say so below.")
    return {
        "package": package,
        "verdict": verdict,
        "reason": reason,
        "duplicate_of": duplicate_of,
        "labels": labels,
        "matches": [b["repo"] for b in bundles],
        "comment": MARKER + "\n" + "\n\n".join(lines) if lines else "",
    }


if __name__ == "__main__":
    number = int(sys.argv[1])
    body = Path(sys.argv[2]).read_text(encoding="utf-8")
    requests = json.loads(Path(sys.argv[3]).read_text(encoding="utf-8"))
    patches_list = json.loads(Path(sys.argv[4]).read_text(encoding="utf-8"))
    registry_path = Path(sys.argv[5]) if len(sys.argv) > 5 else None
    registry = None
    if registry_path and registry_path.is_file():
        try:
            registry = json.loads(registry_path.read_text(encoding="utf-8"))
        except ValueError:
            pass
    title = next((r.get("title", "") for r in requests if r["number"] == number), "")
    print(json.dumps(decide(number, body, requests, patches_list, registry, title)))
