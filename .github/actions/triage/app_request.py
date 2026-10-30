#!/usr/bin/env python3
"""python3 app_request.py <issue-number> <issue-body-file> <requests.json> <patches-list.json> [bundles.json] [elsewhere-dir] [previous-comment]"""

import hashlib
import json
import os
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, str(Path(__file__).parent))
from fields import code_span, field, normalize, parse_fields
from replies import ALL_FORMS

MARKER = "<!-- app-request-bot facts={} -->"
REPO = os.environ.get("GITHUB_REPOSITORY", "")
TITLE_PREFIX = re.compile(r"^\s*(?:\[[^\]]{0,30}\]|app\s*request\b)\s*[:|\-\u2013\u2014\uff1a]?\s*", re.I)
MAX_TITLE = 256
URL = re.compile(r"https?://\S+")
MARKDOWN_LINK = re.compile(r"\[([^\[\]]*)\]\([^)]*\)")
REGISTRY_PAGE = "https://morphe-patches.software/#apps"
REPO_PATH = re.compile(r"[\w.-]+/[\w.-]+")
PACKAGE = re.compile(r"\b[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+\b")
DOMAIN_ENDINGS = {"com", "net", "org", "io", "ph", "me", "app", "co", "in", "tv", "apk", "xyz", "to"}
NAME_NOISE = {"app", "android", "apk", "for", "the"}
EDITION_WORDS = {"pro", "premium", "lite", "free", "paid", "plus", "full", "donate", "unlocked", "key", "adfree", "gold",
                 "vip", "prime", "ad", "no", "ads"}
GENERIC_APP_WORDS = {"movies", "music", "player", "reader", "video", "videos", "radio", "calendar", "camera", "editor",
                     "browser", "launcher", "manager", "notes", "photo", "photos", "scanner", "tracker", "weather",
                     "messenger", "keyboard", "books", "ebook", "stream", "streaming", "mobile", "maps", "translate",
                     "dictionary"}
REQUEST_BOILERPLATE = re.compile(r"\[[^\]]*request[^\]]*\]|\b(feat|feature|bug|request|app request|new app|patch request|"
                                 r"patch|unlock(ing)?|add|support for|please|premium|pro|for)\b|[\[\]():]", re.I)
LOGIN = re.compile(r"[A-Za-z0-9-]+")
BUG_TITLE = re.compile(r"\W*(bug|not working|broken|crash)", re.I)
POSTED_LINK = re.compile(r"\[([\w.-]+/[\w.-]+)#(\d+)\]\(https://github\.com/")
STORE_QUERY = {"play.google.com": "id", "h5-api.aoneroom.com": "p"}
MAX_LISTED = 5
QUIET_DAYS = 14
OPEN_LIMIT = 3
EXEMPT_ASSOCIATIONS = {"OWNER", "MEMBER", "COLLABORATOR"}
CHECKBOX = re.compile(r"^\s*[-*]\s+\[([ xX])\]", re.M)


def is_package(text):
    parts = text.split(".")
    return bool(PACKAGE.fullmatch(text)) and not (len(parts) == 2 and parts[1].lower() in DOMAIN_ENDINGS)


def store_package(url):
    try:
        parts = urlparse(url)
    except ValueError:
        return ""
    value = parse_qs(parts.query).get(STORE_QUERY.get(parts.hostname), [""])[0]
    linked = PACKAGE.match(value)
    return linked.group(0) if linked else ""


def find_package(body):
    fields = parse_fields(body)
    declared = field(fields, "Package name", "Package").strip().strip("`").strip()
    if is_package(declared):
        return declared
    return next(filter(None, map(store_package, URL.findall(field(fields, "App")))), "")


def name_words(raw):
    raw = re.split(r"[,(:]| - ", URL.sub(" ", raw), maxsplit=1)[0]
    return [w for w in map(normalize, re.findall(r"\w+", raw)) if w and w not in NAME_NOISE]


def comparable_name(raw):
    return "".join(name_words(raw))


def app_name(body, title=""):
    return comparable_name(field(parse_fields(body), "App") or TITLE_PREFIX.sub("", title))


def requested_words(body, title=""):
    for raw in (field(parse_fields(body), "App"), TITLE_PREFIX.sub("", title)):
        words = [w for w in name_words(raw) if w not in EDITION_WORDS]
        if any(w not in GENERIC_APP_WORDS for w in words):
            return words
    return []


def same_word(a, b):
    short, long = sorted((a, b), key=len)
    return a == b or (len(long) - len(short) == 1 and long.startswith(short) and long[-1] != "s")


def brand_at(words):
    return next((i for i, w in enumerate(words) if w not in GENERIC_APP_WORDS), None)


def names_same_app(other, package, ours):
    title = str(other.get("title") or "")
    if BUG_TITLE.match(title) or any("bug" in str(label).lower() for label in other.get("labels") or []):
        return False
    text = f"{title} {other.get('body') or ''}".lower()
    if package and re.search(rf"(?<![\w.]){re.escape(package.lower())}(?!\.?\w)", text):
        return True
    theirs = [w for w in map(normalize, re.findall(r"\w+", REQUEST_BOILERPLATE.sub(" ", title)))
              if w and w not in NAME_NOISE | EDITION_WORDS]
    i, j = brand_at(ours), brand_at(theirs)
    if i is None or j is None or len(ours[i]) < 5 or not same_word(ours[i], theirs[j]):
        return False
    following = ours[i + 1:i + 2] + theirs[j + 1:j + 2]
    return len(following) < 2 or same_word(*following)


def cross_posts(registry, package, ours, elsewhere, previous=""):
    listed = {str(b.get("repo")).lower() for b in registry["bundles"] if isinstance(b, dict)}
    owner = REPO.split("/")[0].lower()
    kept = {(repo, int(number)) for repo, number in POSTED_LINK.findall(previous)}
    kept |= {(o["repo"], o["number"]) for o in elsewhere
             if isinstance(o, dict) and REPO_PATH.fullmatch(str(o.get("repo"))) and isinstance(o.get("number"), int)
             and names_same_app(o, package, ours)}
    return sorted((repo, number) for repo, number in kept
                  if repo.lower() in listed and repo.split("/")[0].lower() != owner)


def shown_name(body):
    raw = URL.sub(" ", MARKDOWN_LINK.sub(r"\1", field(parse_fields(body), "App")))
    return next(iter(raw.splitlines()), "").strip(" \t,;:|`-\u2013\u2014")


def canonical_title(title, body):
    rest = TITLE_PREFIX.sub("", title).strip() or shown_name(body) or find_package(body)
    return rest[:MAX_TITLE] if rest else title


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


def listed_bundles(registry, package):
    return sorted(
        (b for b in registry["bundles"]
         if isinstance(b, dict) and REPO_PATH.fullmatch(str(b.get("repo"))) and b["repo"].lower() != REPO.lower()
         and isinstance(b.get("targetApps"), list) and package in b["targetApps"]),
        key=lambda b: -b["stars"] if isinstance(b.get("stars"), int) else 0,
    )


def bundle_name(bundle):
    return re.sub(r"[^A-Za-z0-9 .'+&-]", "", str(bundle.get("name") or ""))[:60].strip() or bundle["repo"]


def author(issue):
    return (issue.get("author") or {}).get("login") or ""


def missing_form_parts(body):
    fields = parse_fields(body)
    missing = [f"the {name} field" for name in ("App", "Requested features") if not field(fields, name)]
    boxes = CHECKBOX.findall(field(fields, "Acknowledgements"))
    if not boxes or " " in boxes:
        missing.append("the checked acknowledgements")
    return missing


def open_requests_at_limit(this, requests):
    login = author(this)
    others = [r for r in requests if login and r is not this and r["state"] == "OPEN" and author(r) == login]
    return sorted(others, key=lambda r: r["number"]) if len(others) >= OPEN_LIMIT else []


def decide(number, body, requests, patches_list, registry=None, title="", now=None, elsewhere=(), previous="",
           enforce_limit=False):
    package = find_package(body)
    name = app_name(body, title)
    supported = supported_apps(patches_list)
    earlier = sorted((r for r in requests if r["number"] < number), key=lambda r: r["number"])
    same_package = [r for r in earlier if package and find_package(r["body"] or "").lower() == package.lower()]
    open_same = [r for r in same_package if r["state"] == "OPEN"]
    verdict, reason, duplicate_of, labels, lines = "pass", "", 0, [], []

    if package and package.lower() in supported:
        verdict, reason = "close", "completed"
        app = supported[package.lower()]
        lines.append(
            f"{app} (`{package}`) is already supported. This discussion is now closed. "
            f"For anything else about {app}, open an issue:\n\n{ALL_FORMS}"
        )
    elif open_same:
        canonical = open_same[0]
        verdict, reason, duplicate_of = "close", "duplicate", canonical["number"]
        lines.append(
            f"Thanks for the request! {link(canonical)} already asks for `{package}`, so this discussion is now "
            f"closed as a duplicate. Please upvote {link(canonical)} instead."
        )
    elif same_package:
        prior = same_package[-1]
        verdict, labels = "flag", ["duplicate"]
        lines.append(
            f"{link(prior)} already asked for `{package}` and was closed. Please reply here with what this request adds."
        )

    this = next((r for r in requests if r["number"] == number), {})
    missing = missing_form_parts(body)
    if verdict == "pass" and missing and this.get("association") not in EXEMPT_ASSOCIATIONS:
        verdict, reason, labels = "close", "no form", ["app request"]
        parts = f"{', '.join(missing[:-1])} and {missing[-1]}" if len(missing) > 1 else missing[0]
        lines.append(
            f"This request didn't use the app request form, so it's missing {parts}. "
            "This discussion is now closed and locked. Please open a new request with the form:\n\n"
            f"https://github.com/{REPO}/discussions/new?category=app-requests"
        )

    if verdict == "close" and discussed_recently(this, now or datetime.now(timezone.utc)):
        verdict, reason, duplicate_of, labels, lines = "hold", "", 0, [], []

    open_by_author = open_requests_at_limit(this, requests) if enforce_limit and verdict != "close" else []
    if open_by_author:
        verdict, reason, duplicate_of, labels = "close", "not planned", 0, []
        rows = [f"- [#{r['number']}](https://redirect.github.com/{REPO}/discussions/{r['number']}) "
                f"{code_span(r.get('title') or '')}" for r in open_by_author]
        lines = [f"You have {len(rows)} app requests open, the maximum per person:\n\n"
                 + "\n".join(rows)
                 + "\n\nThis discussion is now closed. To request this app again, either:\n\n"
                 "- close one of the requests above\n- wait until one of them is added or rejected"]

    if verdict == "pass" and name:
        lookalikes = [r for r in earlier if r["state"] == "OPEN" and r not in same_package
                      and app_name(r["body"] or "", r.get("title", "")) == name]
        lookalikes += [{"number": 0, "name": n} for p, n in supported.items()
                       if comparable_name(n) == name and p != (package or "").lower()]
        if lookalikes:
            verdict = "flag"
            if "duplicate" not in labels:
                labels.append("duplicate")
            apps = " or ".join(r["name"] for r in lookalikes if not r["number"])
            requests = " or ".join(link(r) for r in lookalikes if r["number"])
            if apps:
                lines.append(
                    f"This looks like {apps}, which this bundle already supports. If it's a different app, edit this "
                    f"request and add its package name.\n\nIf it's {apps}, open an issue instead:\n\n{ALL_FORMS}"
                )
            if requests:
                lines.append(
                    f"This looks like the app requested in {requests}. If it's a different app, edit this request and "
                    f"add its package name. If it's the same app, please upvote {requests} instead."
                )

    bundles = []
    if package and verdict in ("pass", "flag") and registry:
        bundles = listed_bundles(registry, package)
        if bundles:
            host = {"github": "https://github.com", "gitlab": "https://gitlab.com"}
            rows = [
                f"- [{bundle_name(b)}]({host.get(b.get('source'), host['github'])}/{b['repo']})"
                for b in bundles[:MAX_LISTED]
            ]
            if len(bundles) > MAX_LISTED:
                rows.append(f"- [{len(bundles) - MAX_LISTED} more]({REGISTRY_PAGE})")
            if verdict == "pass":
                verdict = "flag"
            labels += [label for label in ("needs info", "other bundle") if label not in labels]
            if len(bundles) == 1:
                lines.append(
                    f"{rows[0][2:]} already patches `{package}`. To keep work from being split across bundles, a request "
                    "for an app another bundle covers is kept open only for what that bundle lacks.\n\n"
                    "- **It doesn't support your app version, or a patch broke:** open an issue on that bundle\n"
                    "- **It's missing a feature you need:** reply here and name the feature\n\n"
                    "A reply doesn't guarantee the request gets picked up."
                )
            else:
                lines.append(
                    f"These bundles already patch `{package}`:\n\n" + "\n".join(rows)
                    + "\n\nTo keep work from being split across bundles, a request for an app other bundles cover "
                    "is kept open only for what none of them has.\n\n"
                    "- **They don't support your app version, or a patch broke:** open an issue on one of them\n"
                    "- **None of them has a feature you need:** reply here and name the feature\n\n"
                    "A reply doesn't guarantee the request gets picked up."
                )

    posts = []
    if verdict in ("pass", "flag") and registry:
        posts = cross_posts(registry, package, requested_words(body, title), elsewhere, previous)[:MAX_LISTED]
        if posts:
            verdict = "flag"
            if "other bundle" not in labels:
                labels.append("other bundle")
            refs = ", ".join(f"[{repo}#{number}](https://github.com/{repo}/issues/{number})" for repo, number in posts)
            lines.append(
                f"This app is also requested at {refs}. Keeping one request open helps developers avoid building the "
                "same patches twice.\n\n- **Fine with the other one:** close this discussion\n"
                f"- **Prefer this one:** close {'that one' if len(posts) == 1 else 'the others'} and reply here\n\n"
                "Thanks for helping keep requests in one place!"
            )

    facts = hashlib.sha256(json.dumps([
        package, verdict, reason, duplicate_of, sorted(labels), [b["repo"] for b in bundles], posts,
        [r["number"] for r in open_by_author], [line.split("\n")[0] for line in lines if line.startswith("This looks like")],
    ]).encode()).hexdigest()[:12]
    return {
        "package": package,
        "verdict": verdict,
        "facts": facts,
        "reason": reason,
        "duplicate_of": duplicate_of,
        "labels": labels,
        "matches": [b["repo"] for b in bundles],
        "cross_posts": [f"{repo}#{number}" for repo, number in posts],
        "title": canonical_title(title, body),
        "comment": MARKER.format(facts) + "\n" + "\n\n".join(lines) if lines else "",
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
    if not (isinstance(registry, dict) and isinstance(registry.get("bundles"), list)):
        registry = None
    this = next((r for r in requests if r["number"] == number), {})
    login = author(this)
    posted = Path(sys.argv[6]) / f"{login}.jsonl" if len(sys.argv) > 6 and LOGIN.fullmatch(login) else None
    elsewhere = []
    if posted and posted.is_file():
        elsewhere = [json.loads(line) for line in posted.read_text(encoding="utf-8").splitlines() if line.strip()]
    previous = Path(sys.argv[7]).read_text(encoding="utf-8") if len(sys.argv) > 7 else ""
    enforce_limit = os.environ.get("ACTION") == "created" and os.environ.get("ASSOCIATION") not in EXEMPT_ASSOCIATIONS
    print(json.dumps(decide(number, body, requests, patches_list, registry, this.get("title", ""),
                            elsewhere=elsewhere, previous=previous, enforce_limit=enforce_limit)))
