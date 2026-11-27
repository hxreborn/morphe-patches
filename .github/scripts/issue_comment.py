#!/usr/bin/env python3
"""python3 issue_comment.py [--needs-info] [--unreleased] [--fixed] <issue_comment-event.json> [patches-list.json]"""

import json
import os
import re
import sys
from datetime import datetime, timedelta
from pathlib import Path

from versions import targets
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "actions" / "triage"))
from replies import ALL_FORMS, bundle, code_list
from fields import asks_for_additional_feature

REPO = os.environ.get("GITHUB_REPOSITORY", "hxreborn/morphe-patches")
CLOSED_MARKER = "<!-- closed-issue-bot -->"
PING_MARKER = "<!-- status-ping-bot -->"
UNRELEASED_MARKER = "<!-- unreleased-issue-bot -->"
NO_REPLY = 3
PING_WINDOW = timedelta(days=30)
LOCK_DELAY = timedelta(days=8)
CLOSE_WITH_COMMENT_WINDOW = timedelta(seconds=30)
COLLABORATORS = {"OWNER", "MEMBER", "COLLABORATOR"}
SUPPORT_NOTES = f"https://github.com/{REPO}/blob/main/.github/SUPPORT.md#good-to-know"

REQUEST = re.compile(
    r"\b(update|upgrade|version|latest|newest|new|support|please|pls|plz|can you|could you|add|bring|fix|solutions?"
    r"|any news|eta)\b"
    r"|\b(not|doesn'?t|does not|didn'?t|did not|won'?t|isn'?t|stopped|no longer) work"
    r"|\b(still|broken|crash|error|fail|failure|missing|alert|popup|toast|dialog)",
    re.I,
)
PROBLEM = re.compile(
    r"\b(not|doesn'?t|does not|didn'?t|did not|won'?t|isn'?t|stopped|no longer) work"
    r"|\b(still|again|broken|crash|error|fail|failure|missing)",
    re.I,
)
UPDATE_DEMAND = re.compile(
    r"\b(?:please|pls|plz|can you|could you)\s+(?:also\s+)?(?:update|upgrade)\b(?!\s+(?:me|us)\b)"
    r"|\b(?:update|upgrade)\s+(?:patches?\s+)?(?:to|for)\s+v?\d"
    r"|\b(?:support|add)\s+(?:for\s+)?(?:the\s+)?(?:latest|newest|newer|new)\s+(?:app\s+)?version\b",
    re.I,
)
RESOLVED = re.compile(r"\b(work(s|ing)? (fine|perfectly|great|now|as expected)|now (it )?works|it works|fixed now|solved)\b", re.I)
QUOTED = re.compile(r"^[ \t]*>.*$", re.M)
MENTION = re.compile(r"@[\w-]+")
OWNER_MENTION = re.compile(rf"@{re.escape(REPO.split('/')[0])}\b", re.I)
CODE = re.compile(r"```.*?(?:```|\Z)|`[^`]*`|<!--.*?(?:-->|\Z)", re.S)
DIAGNOSTIC_REPLY = r"(?!\s+(to|whether|if|my|the|this|on|with|about)\b)"
PING = re.compile(
    r"\bany\s+(updates?|news|progress)\b(?!\s+(of|to|from)\b)"
    r"|\bany\s+(chance|possibility)\s+(of|for)\s+(an?\s+)?update"
    r"|\b(are|is)\s+(you|anyone|someone|somebody)\s+(still\s+)?working\s+on\s+(it|this)\b"
    r"|\bworking\s+on\s+(it|this)\s+or\s+not\b"
    r"|\beta\b"
    r"|\bwhen\s+will\b"
    r"|\bstill\s+(alive|planned)\b"
    r"|\bsay\s+something\b"
    r"|\b(has|is)\s+(this|it)\s+(been\s+)?(added|done|implemented|ready)\s+yet\b"
    r"|\b(patiently|desperately)\s+waiting\b|\bwaiting\b[^.?!]{0,20}\b(patiently|desperately)\b"
    r"|\b(don'?t|do\s+not)\s+forget\s+(about\s+)?(us|me|this|it)\b"
    r"|\b(dev|you|maintainer)\s+(didn'?t|did\s+not|hasn'?t|has\s+not|haven'?t)\s+(even\s+)?(see|look|repl|respon)"
    r"|\b(could|can|would|will)\s+(you|someone|anyone)\s+((at\s+least|please|pls|plz)\s+)*"
    r"((provide|give|post|leave|send)\s+((us|me)\s+)?(an?|any|some)\s+)?(reply|response|answer)\b"
    rf"{DIAGNOSTIC_REPLY}"
    rf"|\bat\s+least\s+(reply|respond|answer)\b{DIAGNOSTIC_REPLY}"
    r"|\bignor(e|ed|ing)\s+(us|me|this|my|our)\b"
    rf"|\b(please|pls|plz|kindly)\s+(reply|respond|answer)\b{DIAGNOSTIC_REPLY}"
    r"|\b(reply|respond|answer)\s+(please|pls|plz)\b"
    r"|\b(still|yet)\s+no\s+(reply|response|answer|update|news|word)\b"
    r"|\bno\s+(reply|response|answer|update|news)\s+(yet|at\s+all|since|for\s+(days|weeks|months))\b"
    r"|\b(is|has)\s+(this|it|the)(\s+(project|request|repo))?\s+(been\s+)?(dead|abandoned|forgotten)\b"
    r"|\bhow\s+much\s+longer\b"
    r"|\bhow\s+long\s+(do|will|should|must)\s+(we|i)\s+(have\s+to\s+)?wait\b"
    r"|\b(i'?m|we'?re|i\s+am|we\s+are|everyone'?s|everyone\s+is)\s+still\s+waiting\b"
    r"|^\s*still\s+waiting\b"
    r"|\b(it'?s|it\s+has|its)\s+been\s+((\d+|a|an|one|two|three|four|several|many|few)\s+(days?|weeks?|months?)|a\s+while|ages|forever)"
    r"(\s+(already|now|since\s+(this|the\s+request|i\s+(asked|requested|posted))))?\s*([.?!,]|$)"
    r"|\b(any\s*one|any\s*body|some\s*one|some\s*body)\s+(here|there|home|alive)\s*\?"
    r"|\bare\s+you\s+(still\s+)?(there|here|alive|around)\b|^\s*(you|u)\s+there\b"
    r"|\bwill\s+(this|it)\s+(ever\s+)?(be\s+)?(added|supported|done|implemented|patched|happen)\b(?!\s+(if|when|for|by|with|on|in)\b)"
    r"|\b(is|was)\s+(this|it)\s+(ever\s+)?(going\s+to|gonna)\s+(happen|be\s+(added|done|supported|implemented|patched))\b"
    r"|\b(still\s+nothing|nothing\s+yet)\b"
    r"|\bhurry\s+up\b"
    r"|\b(what\s+are\s+your|any)\s+thoughts\s+(on|about|with)\s+(this|it)\b|\bany\s+thoughts\s*\?"
    r"|\b(please|pls|plz|kindly)\s+(start\s+)?work(ing)?\s+on\s+(it|this)\b"
    r"|\b(please|pls|plz|need\s+(this|it)|do\s+(this|it)|add\s+(this|it))\b[^.?!]{0,25}\b(asap|as\s+soon\s+as\s+possible)\b"
    r"|\bwhen\s+(is|are)\s+(this|it|these|they)\s+(getting|going\s+to\s+be|gonna\s+be)\s+(released|added|done|out|fixed|patched|updated|supported)\b"
    r"|\bwaiting\s+for\s+(an?\s+|the\s+|your\s+|any\s+)?(update|reply|response|answer|news|patch)e?s?\b"
    r"(?=\s*([.?!,]|$|on\s+this|for\s+this|from\s+(you|the\s+dev)))",
    re.I,
)
RELEASE_QUESTION = re.compile(
    r"\bwhen\b[^.?!]{0,40}\b(releas\w*|ship\w*|built|build|out|live|available|coming)\b"
    r"|\b(releas\w*|build)\b[^.?!]{0,30}\b(yet|soon|date|eta)\b"
    r"|\bnext\s+(release|build)\b",
    re.I,
)
NO_RUSH = re.compile(r"\bno\s+rush\b|\btake\s+your\s+time\b|\bno\s+pressure\b|\bnot\s+to\s+put\s+any\s+pressure\b", re.I)
BUMP_WORDS = {"bump", "bumping", "bumped", "up", "ping", "waiting"}
PATCH_WORDS = {"patch", "patches"}
PLEASE_WORDS = {"please", "pls", "plz"}
BUMP_EXTRAS = {"again"} | PATCH_WORDS | PLEASE_WORDS
GREETINGS = {(), ("hello",), ("hi",), ("hey",), ("anyone",), ("hello", "anyone")}
STATUS_WORDS = {("update",), ("updates",), ("news",), ("status",), ("progress",), ("eta",), ("when",), ("soon",)}
GREETING_FILLER = {"hey", "hi", "hello", "yo", "sir", "bro", "brother", "boss", "dev"}
ADD_REQUEST_WORDS = {"please", "pls", "plz", "kindly", "add", "this", "it", "app", "patch", "patches", "support", "for"}
MENTION_FILLER = {"hey", "hi", "hello", "yo", "bro", "brother", "sir", "dev", "boss", "please", "pls", "plz", "kindly",
                  "bump", "up", "again", "reply", "answer", "respond", "ping"}


def normalize(text):
    return re.sub(r"[^a-z0-9]", "", text.lower())


def targeted_app(text, apps):
    haystack = normalize(text)
    names = [name for name in apps if normalize(name) and normalize(name) in haystack]
    return max(names, key=len, default="")


def reported_app(title, body, patches_list):
    apps = targets(patches_list)
    packages = {
        package["packageName"]: package["name"]
        for patch in patches_list["patches"]
        for package in patch.get("compatiblePackages") or []
        if package.get("packageName") and package["name"] in apps
    }
    named = {name for package, name in packages.items() if re.search(rf"(?<![\w.]){re.escape(package)}(?![\w.])", body)}
    return named.pop() if len(named) == 1 else targeted_app(title, apps)


def is_request(comment):
    own = CODE.sub("", QUOTED.sub("", comment))
    return bool(REQUEST.search(own)) and not RESOLVED.search(own)


def is_closing_comment(issue, comment):
    closed_at = issue.get("closed_at")
    return (
        bool(closed_at)
        and (issue.get("user") or {}).get("login") == (comment.get("user") or {}).get("login")
        and abs(timestamp(comment["created_at"]) - timestamp(closed_at)) <= CLOSE_WITH_COMMENT_WINDOW
    )


def is_fix_follow_up(issue, comment):
    labels = {label["name"] for label in issue.get("labels") or []}
    own = CODE.sub("", QUOTED.sub("", comment["body"] or ""))
    closed_at = issue.get("closed_at")
    return (
        bool(labels & {"bug", "enhancement"})
        and bool(closed_at)
        and timedelta(0) <= timestamp(comment["created_at"]) - timestamp(closed_at) <= LOCK_DELAY
        and bool(PROBLEM.search(own))
        and not UPDATE_DEMAND.search(own)
        and not asks_for_additional_feature(own)
    )


def squeeze(word):
    return re.sub(r"(\w)\1+", r"\1", word.lower())


def squeezed_words(text):
    return tuple(squeeze(word) for word in re.findall(r"\w+", text))


def squeezed_set(words):
    return {squeeze(word) for word in words}


def is_bare_ping(text):
    words = squeezed_words(text)
    bump = squeezed_set(BUMP_WORDS)
    if words and bump & set(words) and set(words) <= bump | squeezed_set(BUMP_EXTRAS):
        return True
    if len(words) == 2 and words[0] in squeezed_set(PATCH_WORDS) and words[1] in squeezed_set(PLEASE_WORDS):
        return True
    if words and set(words) <= squeezed_set(GREETING_FILLER):
        return True
    if squeeze("add") in words and set(words) <= squeezed_set(ADD_REQUEST_WORDS):
        return True
    questions = {tuple(squeeze(word) for word in phrase) for phrase in GREETINGS | STATUS_WORDS}
    return "?" in text and words in questions


def is_filler_only(text):
    return set(squeezed_words(text)) <= squeezed_set(MENTION_FILLER)


def is_status_ping(comment):
    own = QUOTED.sub("", comment)
    unmentioned = MENTION.sub("", own).strip()
    if NO_RUSH.search(unmentioned):
        return False
    visible = CODE.sub("", unmentioned)
    if PING.search(visible):
        return True
    if visible == unmentioned and is_bare_ping(unmentioned):
        return True
    return OWNER_MENTION.search(own) is not None and visible == unmentioned and is_filler_only(unmentioned)


def is_release_question(comment):
    own = CODE.sub("", QUOTED.sub("", comment))
    return is_status_ping(comment) or bool(RELEASE_QUESTION.search(own))


def timestamp(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def is_app_request(issue):
    return any(label["name"] == "app request" for label in issue.get("labels") or [])


def gets_ping_reply(issue, comment):
    if not is_status_ping(comment["body"] or ""):
        return False
    age = timestamp(comment["created_at"]) - timestamp(issue["created_at"])
    return is_app_request(issue) or age <= PING_WINDOW


def answers_needs_info(issue, comment):
    return (
        issue["state"] == "open"
        and any(label["name"] == "needs info" for label in issue.get("labels") or [])
        and comment["user"]["login"] == issue["user"]["login"]
        and comment["user"]["type"] != "Bot"
        and comment["author_association"] not in COLLABORATORS
    )


def ping_reply(issue):
    if any(label["name"] == "in pre-release" for label in issue.get("labels") or []):
        return (
            f"{PING_MARKER}\n"
            "This change has been added in a pre-release, see the comment above. Please try it and reply whether it works. "
            "The next stable release will include it.\n"
        )
    upvote = " Upvoting the request shows demand." if is_app_request(issue) else ""
    return (
        f"{PING_MARKER}\n"
        "There's no ETA for this request. This is a hobby project run by one developer, and pings don't change the order, "
        f"see [Good to know]({SUPPORT_NOTES}).{upvote} Any progress will be posted here.\n"
    )


def unreleased_reply():
    return (
        f"{UNRELEASED_MARKER}\n"
        "The fix isn't in a release yet. A comment here will name the release that includes it.\n"
    )


def closed_issue_reply(commented_at, issue_title="", patches_list=None, keep_unlocked=False, issue_body=""):
    lock_day = commented_at + LOCK_DELAY
    lead = "This issue is closed. For anything new, please open a new issue:"
    if patches_list:
        apps = targets(patches_list)
        app = reported_app(issue_title, issue_body, patches_list)
        if app and apps[app]:
            lead = f"{bundle(patches_list['version'])} supports {app} {code_list(apps[app])}. {lead}"
    return (
        f"{CLOSED_MARKER}\n"
        f"{lead}\n"
        "\n"
        f"{ALL_FORMS}\n"
        + ("" if keep_unlocked else
           f"\nThis issue will be locked on {lock_day:%B} {lock_day.day}, {lock_day.year} unless someone comments first.\n")
    )


if __name__ == "__main__":
    flags = {arg for arg in sys.argv[1:] if arg.startswith("--")}
    args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
    event = json.loads(Path(args[0]).read_text(encoding="utf-8"))
    issue, comment = event["issue"], event["comment"]
    body = comment["body"] or ""
    if "--needs-info" in flags:
        sys.exit(0 if answers_needs_info(issue, comment) else NO_REPLY)
    if issue["state"] == "open":
        if not gets_ping_reply(issue, comment):
            sys.exit(NO_REPLY)
        print(ping_reply(issue), end="")
    elif "--unreleased" in flags:
        if not (is_request(body) or is_release_question(body)):
            sys.exit(NO_REPLY)
        print(unreleased_reply(), end="")
    else:
        if (is_closing_comment(issue, comment) or not is_request(body)
                or ("--fixed" in flags and is_fix_follow_up(issue, comment))):
            sys.exit(NO_REPLY)
        patches_list = json.loads(Path(args[1]).read_text(encoding="utf-8")) if len(args) > 1 else None
        labels = {label["name"] for label in issue.get("labels") or []}
        print(closed_issue_reply(timestamp(comment["created_at"]), issue.get("title") or "", patches_list, "no-stale" in labels, issue.get("body") or ""), end="")
