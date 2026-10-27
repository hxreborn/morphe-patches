#!/usr/bin/env python3
"""python3 .github/scripts/test_issue_comment.py"""

import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from issue_comment import (
    answers_needs_info, timestamp, CLOSED_MARKER, PING_MARKER, UNRELEASED_MARKER, is_early_status_ping,
    is_release_question, is_request, is_status_ping, ping_reply, closed_issue_reply, unreleased_reply,
)

REQUESTS = [
    "Update the latest support version bro",
    "Hello, Can you update it for the new 1.4 version?",
    "Hello,  the app just got an update and the patch no longer works with the new version.",
    "Remove rating prompts patch not works, when app open popups stills visible, please check @hxreborn",
    "And one more issue I can't access this app without internet, please add a patches to solve this issue.. thanks",
    "@hxreborn Please add one more patch 'remove app startup delay' thanks again for all previous patches",
    "Hey\nCan you patch this app\n\nhttps://athan-prayer-times-quran-more.en.uptodown.com/android",
    "You close it as completed but terabox has no clone patch please bring terabox clone patch",
    "Didn't work",
    "I dunno if I am doing it wrong, it says 1.47 at the bottom but still only see 7 patches",
    "This alert appears on opening the app",
    "please update patches to 6.83.0",
    "This has been closed without a fix?",
    "Any solutions to this yet?",
    "New version changed a bit, remove review in mainactivity changed class. Library patch also changed bytes",
]

NOT_REQUESTS = [
    "Thank you so much for taking my suggestion; I am very grateful. Best regards.",
    "Bro, you're a godsend ❤️ Thanks for all the hard work! 🙏",
    "Many thanks",
    "you are fast, my friend. thank you very much, sir",
    "Thank you ! it s fixed now !!!",
    "Fixed after reinstalling app\n👍",
    "Yes, it's perfect.",
    "Great. Thank you very much.",
    "Completed in hxreborn patches 1.47.0",
    "Yeah thanks , Updated and working perfectly now .",
    "No problem. Thanks for the work you and other devs are doing here in your spare time.",
    "> This is included in [v1.34.0](https://github.com/x/releases/tag/v1.34.0). Update the bundle in Morphe Manager and repatch.\n\nThis is actually crazy automation bro.",
    "> Thanks for the request! This is in v1.31.0. Update the bundle and repatch, and comment below if something is missing.\n\nnice",
    "",
]

failures = 0
checks = 0


def record(ok):
    global failures, checks
    checks += 1
    failures += not ok


for text in REQUESTS:
    ok = is_request(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} replies to: {text[:70]!r}")
for text in NOT_REQUESTS:
    ok = not is_request(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} silent on: {text[:70]!r}")

PINGS = [
    "@hxreborn say something please, are you working on this or not..",
    "@hxreborn patches please..",
    "Why the dev didn't see this yet I also want this patch",
    "any update?",
    "Any updates on this?",
    "eta?",
    "is this still alive?",
    "bump",
    "?",
    "When will this be added?",
    "Is anyone working on this?",
    "@hxreborn say something",
    "@hxreborn",
    "@hxreborn 🙏",
    "hello?",
    "Waiting for this patiently..",
    "Most needed feature. Desperately waiting",
    "please do not forget about us",
    "any possibility of an update on this?",
    "Has this been added yet I think it would be helpful",
    "ANY UPDATE???",
]

NOT_PINGS = [
    "Yes, please",
    "Ok, got it. I will wait for the fix!",
    "Thanks anyway for looking into it. 😊\nIs there a chance that future updates will change that?",
    "Hello, Can you update it for the new 1.4 version?",
    "@hxreborn Please add one more patch 'remove app startup delay' thanks again for all previous patches",
    "Thank you @hxreborn please add one more patch to remove startup ad (pan masala banner)",
    "This app is not working properly, it needs a new patch.",
    "It could be rather good, also a custom accent color.",
    "did it work for you after the 1.19.0 update?",
    "Got it bro, that makes much more sense now. Thanks for figuring out the actual cause and working on the fix.",
    "Not working",
    "I'm having the same issue, and it's still happening with v3.3.0-dev.1",
    "Does any update of the app break the patch?",
    "```\nI/Download eta 12s\n```",
    "Sure, waiting for dev.12 release",
    "Don't forget morphe is a team of many devs",
    "@hxreborn here is the logcat you asked for",
    "+1",
    "@someone-else",
    "👍",
    "...",
    "Please take your time; there's no rush. We'll be waiting patiently for you.",
    "Any update? Just wanted to let you know, not to put any pressure on you.",
    "? `Morphe > Player > Force landscape in fullscreen`",
    "@hxreborn `FATAL EXCEPTION: main java.lang.NoSuchMethodError`",
    "Full localization support has not been implemented yet",
    "> any update?\n\nthanks, works on 1.52.0",
    "<!-- any updates? -->",
    "<!-- bump -->",
    "`bump`",
    "```\nany updates?",
    "",
]

for text in PINGS:
    ok = is_status_ping(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} answers ping: {text[:70]!r}")
for text in NOT_PINGS:
    ok = not is_status_ping(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} silent on: {text[:70]!r}")

ISSUE = {"created_at": "2026-10-09T06:22:46Z"}
ok = is_early_status_ping(ISSUE, {"created_at": "2026-10-13T10:00:00Z", "body": PINGS[0]})
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a ping 4 days after opening gets the reply")

ok = not is_early_status_ping(ISSUE, {"created_at": "2026-11-09T06:22:47Z", "body": PINGS[0]})
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a ping after 30 days gets no reply")

text = ping_reply({"labels": []})
ok = text == PING_MARKER + "\nThere's no ETA for this request yet. Any progress will be posted here.\n"
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the ping reply is the marker and the no-ETA sentence")

text = ping_reply({"labels": [{"name": "in pre-release"}]})
ok = text == PING_MARKER + ("\nThis change has been added in a pre-release, see the comment above. Please try it and reply "
                            "whether it works. The next stable release will include it.\n")
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a ping on an issue in pre-release points at the pre-release comment")

PATCHES = {"version": "1.49.1", "patches": [{"compatiblePackages": [
    {"name": "Proton VPN", "packageName": "ch.protonvpn.android", "targets": [{"version": "5.20.39.0"}, {"version": "5.20.57.0"}]},
    {"name": "Proton Mail", "packageName": "ch.protonmail.android", "targets": [{"version": "7.11.10"}]},
]}]}

COMMENTED_AT = timestamp("2026-10-15T10:00:00Z")
text = closed_issue_reply(COMMENTED_AT)
ok = text.startswith(CLOSED_MARKER + "\n") and all(f"template={t}.yml" in text for t in
                                                  ("bug_report", "not_working", "feature_request", "app_update"))
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the closed-issue reply carries its marker and all four forms")

ok = text.rstrip().endswith("This issue will be locked on October 23, 2026 unless someone comments first.")
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the closed-issue reply names the day the lock job locks it")

text = closed_issue_reply(COMMENTED_AT, "[App Update]: Proton VPN 5.20.57.0", PATCHES)
ok = text.splitlines()[1] == ("Bundle [v1.49.1](https://github.com/hxreborn/morphe-patches/releases/tag/v1.49.1) supports Proton VPN "
                               "`5.20.39.0` and `5.20.57.0`. This issue is closed. For anything new, please open a new issue:")
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a title naming a supported app opens with its targeted versions")

text = closed_issue_reply(COMMENTED_AT, "[Feature]: Hide Unnecessary boxes", PATCHES)
ok = text.splitlines()[1] == "This issue is closed. For anything new, please open a new issue:"
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a title naming no app keeps the plain opening")

MOVIEBOX = {"version": "1.54.0", "patches": [{"compatiblePackages": [
    {"name": "MovieBox", "packageName": "com.community.oneroom", "targets": [{"version": "4.0.03.0918.03"}]},
    {"name": "MovieBox TV", "packageName": "com.community.mbox.tv", "targets": [{"version": "1.2.0.0923.web_socialmedia"}]},
]}]}
MOVIEBOX_TV_REQUEST = """### App

https://moviebox.ph/moviebox-tv-apk

### Package name

com.community.mbox.tv

### Requested features

Unlock premium
"""
text = closed_issue_reply(COMMENTED_AT, "[App Request]: Moviebox for Android TV", MOVIEBOX, issue_body=MOVIEBOX_TV_REQUEST)
ok = text.splitlines()[1].endswith("supports MovieBox TV `1.2.0.0923.web_socialmedia`. This issue is closed. For anything new, please open a new issue:")
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a package name in the body picks the app over a shorter title match")

text = closed_issue_reply(COMMENTED_AT, "[App Request]: Moviebox for Android TV", MOVIEBOX)
ok = "supports MovieBox `4.0.03.0918.03`" in text.splitlines()[1]
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a body without a package name falls back to the title match")

text = closed_issue_reply(COMMENTED_AT, "[Not working]: Something", MOVIEBOX, issue_body="com.community.oneroom and com.community.mbox.tv")
ok = text.splitlines()[1] == "This issue is closed. For anything new, please open a new issue:"
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} a body naming two apps keeps the plain opening")

RELEASE_QUESTIONS = [
    "when is this getting released?",
    "A new release hasn't been built yet with your commit.",
    "When will the build be out?",
    "is the release out yet",
    "any update?",
    "eta?",
    "@hxreborn",
    "When is the next release",
]
NOT_RELEASE_QUESTIONS = [
    "Thanks, works now",
    "> when is this getting released?\n\nthanks",
    "`when is this released`",
    "Great work, thank you",
    "the release notes list this app",
    "",
]
for text in RELEASE_QUESTIONS:
    ok = is_release_question(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} release question: {text[:70]!r}")
for text in NOT_RELEASE_QUESTIONS:
    ok = not is_release_question(text)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} not a release question: {text[:70]!r}")

ok = unreleased_reply() == UNRELEASED_MARKER + "\nThe fix isn't in a release yet. A comment here will name the release that includes it.\n"
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} the unreleased reply is the marker and one plain line")

started = time.monotonic()
flood = [is_status_ping("?" * 5000 + "x"), is_status_ping("\n" * 60000 + "x"), is_request("\n" * 60000 + "x"),
         is_release_question("when " * 20000), is_release_question("release " * 20000 + "x")]
ok = flood == [False] * 5 and time.monotonic() - started < 2
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} long runs of question marks and newlines are read in linear time")

HERE = Path(__file__).resolve().parent
WORKFLOW = HERE.parent / "workflows" / "issue_comments.yml"

FAKE_GH = r'''#!/usr/bin/env python3
import json, os, re, sys

args = sys.argv[1:]
state = json.load(open(os.environ["FAKE_STATE"]))
state["calls"].append(args)
json.dump(state, open(os.environ["FAKE_STATE"], "w"))
flags = {"--repo", "--remove-label", "--add-label"}
valid = (
    args[:2] == ["issue", "edit"] and len(args) >= 3 and re.fullmatch(r"\d+", args[2])
    and all(a in flags or args[i - 1] in flags for i, a in enumerate(args[3:], 3))
    and "--repo" in args and re.fullmatch(r"[\w.-]+/[\w.-]+", args[args.index("--repo") + 1])
    and "--remove-label" in args and args[args.index("--remove-label") + 1] != ""
)
sys.exit(0 if valid else 1)
'''


def needs_info_script():
    lines = WORKFLOW.read_text().splitlines()
    start = lines.index("  needs-info:")
    begin = next(i for i in range(start, len(lines)) if lines[i].strip() == "run: |") + 1
    end = next((i for i in range(begin, len(lines)) if lines[i] and not lines[i].startswith(" " * 10)), len(lines))
    return "\n".join(line[10:] for line in lines[begin:end])


def run_needs_info(state, author="reporter", commenter="reporter", commenter_type="User", association="NONE", labels=("bug", "needs info")):
    event = {
        "issue": {"number": 7, "state": state, "user": {"login": author},
                  "labels": [{"name": label} for label in labels]},
        "comment": {"user": {"login": commenter, "type": commenter_type}, "author_association": association, "body": "here is the log"},
    }
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        (temp / "bin").mkdir()
        (temp / "bin" / "gh").write_text(FAKE_GH)
        (temp / "bin" / "gh").chmod(0o755)
        (temp / "state.json").write_text(json.dumps({"calls": []}))
        (temp / "event.json").write_text(json.dumps(event))
        env = {"PATH": f"{temp / 'bin'}:{os.environ['PATH']}", "FAKE_STATE": str(temp / "state.json"),
               "GITHUB_EVENT_PATH": str(temp / "event.json"), "ISSUE": "7", "REPO": "hxreborn/morphe-patches"}
        done = subprocess.run(["bash", "-eo", "pipefail", "-c", needs_info_script()], env=env, cwd=HERE.parent.parent, capture_output=True, text=True)
        return done.returncode, json.loads((temp / "state.json").read_text())["calls"]


REMOVE = [["issue", "edit", "7", "--repo", "hxreborn/morphe-patches", "--remove-label", "needs info"]]
NEEDS_INFO_CASES = [
    ("the author's reply removes the label", dict(state="open"), REMOVE),
    ("a reply on a closed issue leaves it", dict(state="closed"), []),
    ("a bot reply leaves it", dict(state="open", commenter="github-actions[bot]", commenter_type="Bot", association="CONTRIBUTOR"), []),
    ("another user's comment leaves it", dict(state="open", commenter="someone-else"), []),
    ("a collaborator's own issue leaves it", dict(state="open", author="hxreborn", commenter="hxreborn", association="OWNER"), []),
    ("an issue without the label leaves it", dict(state="open", labels=("bug",)), []),
]
for name, kwargs, calls in NEEDS_INFO_CASES:
    status, seen = run_needs_info(**kwargs)
    ok = status == 0 and seen == calls
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} needs info: {name}")

FAKE_REPLY_GH = r'''#!/usr/bin/env python3
import json, os, re, sys

args = sys.argv[1:]
state = json.load(open(os.environ["FAKE_STATE"]))
state["calls"].append(args)


def finish(code, out=""):
    json.dump(state, open(os.environ["FAKE_STATE"], "w"))
    sys.stdout.write(out)
    sys.exit(code)


if args[:1] == ["api"]:
    path, rest = args[1], args[2:]
    flags = [a for a in rest if a.startswith("--")]
    if "--slurp" in flags or not set(flags) <= {"--paginate", "--jq"}:
        finish(1)
    if "--jq" not in rest or rest.index("--jq") + 1 >= len(rest):
        finish(1)
    expression = rest[rest.index("--jq") + 1]
    if re.fullmatch(r"repos/[\w.-]+/[\w.-]+/issues/\d+/timeline", path):
        if state.get("timeline_fails"):
            finish(1)
        finish(0, "".join(f"{commit}\n" for commit in state["closing"]))
    if re.fullmatch(r"repos/[\w.-]+/[\w.-]+/issues/\d+/comments", path):
        marker = re.search(r'contains\("(<!--.*?-->)"\)', expression)
        if not marker or "github-actions[bot]" not in expression:
            finish(1)
        finish(0, "".join(f"{i}\n" for i, body in enumerate(state["bot_comments"], 1) if marker.group(1) in body))
    finish(1)
if args[:2] == ["issue", "comment"] and re.fullmatch(r"\d+", args[2]) and "--repo" in args and "--body-file" in args:
    state["posted"].append(open(args[args.index("--body-file") + 1]).read())
    finish(0)
finish(1)
'''

FAKE_GIT = r'''#!/usr/bin/env python3
import json, os, re, sys

args = sys.argv[1:]
state = json.load(open(os.environ["FAKE_STATE"]))
if len(args) == 3 and args[:2] == ["cat-file", "-e"] and re.fullmatch(r"[0-9a-f]{40}\^\{commit\}", args[2]):
    sys.exit(0 if args[2][:40] in state["known"] else 128)
if len(args) == 3 and args[:2] == ["tag", "--contains"] and re.fullmatch(r"[0-9a-f]{40}", args[2]):
    if state.get("tag_fails"):
        sys.exit(128)
    print("".join(f"{tag}\n" for tag in state["tags"].get(args[2], [])), end="")
    sys.exit(0)
sys.exit(1)
'''


def reply_script():
    lines = WORKFLOW.read_text().splitlines()
    start = lines.index("  reply:")
    begin = next(i for i in range(start, len(lines)) if lines[i].strip() == "run: |") + 1
    end = next((i for i in range(begin, len(lines)) if lines[i] and not lines[i].startswith(" " * 10)), len(lines))
    return "\n".join(line[10:] for line in lines[begin:end])


UNRELEASED_SHA = "a" * 40
RELEASED_SHA = "b" * 40
MISSING_SHA = "c" * 40
PRERELEASED_SHA = "d" * 40
SHAS = {"known": [UNRELEASED_SHA, RELEASED_SHA, PRERELEASED_SHA],
        "tags": {RELEASED_SHA: ["v1.2.0"], UNRELEASED_SHA: [], PRERELEASED_SHA: ["v1.3.0-dev.1"]}}


def run_reply(body, state="closed", closing=(), bot_comments=(), labels=("app request",), closed_at="2026-10-18T22:14:42Z",
              commented_at="2026-10-19T12:37:46Z", **extra):
    event = {
        "issue": {"number": 7, "state": state, "title": "[App Request]: Something", "created_at": "2026-10-01T00:00:00Z",
                  "closed_at": closed_at, "labels": [{"name": label} for label in labels]},
        "comment": {"user": {"login": "asker", "type": "User"}, "author_association": "NONE", "body": body, "created_at": commented_at},
    }
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        (temp / "bin").mkdir()
        for name, source in (("gh", FAKE_REPLY_GH), ("git", FAKE_GIT)):
            (temp / "bin" / name).write_text(source)
            (temp / "bin" / name).chmod(0o755)
        (temp / "state.json").write_text(json.dumps({"calls": [], "posted": [], "closing": list(closing), "bot_comments": list(bot_comments), **SHAS, **extra}))
        (temp / "event.json").write_text(json.dumps(event))
        env = {"PATH": f"{temp / 'bin'}:{os.environ['PATH']}", "FAKE_STATE": str(temp / "state.json"), "GITHUB_EVENT_PATH": str(temp / "event.json"),
               "RUNNER_TEMP": str(temp), "ISSUE": "7", "REPO": "hxreborn/morphe-patches", "STATE": state}
        done = subprocess.run(["bash", "-eo", "pipefail", "-c", reply_script()], env=env, cwd=HERE.parent.parent, capture_output=True, text=True)
        final = json.loads((temp / "state.json").read_text())
        return done.returncode, final["posted"], final["calls"]


STILL_BROKEN = "<img width=\"720\" height=\"1412\" alt=\"Image\" src=\"https://github.com/user-attachments/assets/48cee40d\" />\n\nThe issue is still NOT fixed even after the new signin patch. Please check "
CRASHING = "> 18\\. Proceed anyway ! 🤌🏻\n\napp is crashing on launch"
FIXED_BUG = dict(closing=[RELEASED_SHA], labels=("bug",))
ETA = "when is this getting released? A new release hasn't been built yet with your commit."
UNRELEASED_SENT = UNRELEASED_MARKER + "\nx"
REPLY_CASES = [
    ("an ETA question on an unreleased closed issue gets the unreleased line", dict(body=ETA, closing=[UNRELEASED_SHA]), 0, UNRELEASED_MARKER),
    ("an ETA question once the commit is tagged gets the redirect", dict(body=ETA, closing=[RELEASED_SHA]), 0, CLOSED_MARKER),
    ("a commit tagged only by a pre-release still gets the unreleased line", dict(body=ETA, closing=[PRERELEASED_SHA]), 0, UNRELEASED_MARKER),
    ("an issue closed without a commit keeps the redirect", dict(body=ETA, closing=["-"]), 0, CLOSED_MARKER),
    ("a closing commit missing from the clone keeps the redirect", dict(body=ETA, closing=[MISSING_SHA]), 0, CLOSED_MARKER),
    ("the latest close counts after a reopen", dict(body=ETA, closing=[RELEASED_SHA, UNRELEASED_SHA]), 0, UNRELEASED_MARKER),
    ("a close without a commit after a commit close keeps the redirect", dict(body=ETA, closing=[UNRELEASED_SHA, "-"]), 0, CLOSED_MARKER),
    ("a status ping on an unreleased closed issue gets the unreleased line", dict(body="any update?", closing=[UNRELEASED_SHA]), 0, UNRELEASED_MARKER),
    ("a request on an unreleased closed issue gets the unreleased line", dict(body="Didn't work", closing=[UNRELEASED_SHA]), 0, UNRELEASED_MARKER),
    ("thanks on an unreleased closed issue gets no reply", dict(body="Thanks, works great", closing=[UNRELEASED_SHA]), 0, None),
    ("a second question after the unreleased line gets no reply", dict(body=ETA, closing=[UNRELEASED_SHA], bot_comments=[UNRELEASED_SENT]), 0, None),
    ("the unreleased line does not block the redirect once released", dict(body=ETA, closing=[RELEASED_SHA], bot_comments=[UNRELEASED_SENT]), 0, CLOSED_MARKER),
    ("a still-broken report on a bug closed by a released fix gets no reply", dict(body=STILL_BROKEN, closed_at="2026-10-26T09:53:02Z", commented_at="2026-10-26T15:34:24Z", **FIXED_BUG), 0, None),
    ("a crash report on a bug closed by a released fix gets no reply", dict(body=CRASHING, closed_at="2026-10-22T11:43:23Z", commented_at="2026-10-24T05:43:43Z", **FIXED_BUG), 0, None),
    ("a crash report a month after the fix gets the redirect", dict(body=CRASHING, closed_at="2026-09-22T11:43:23Z", commented_at="2026-10-24T05:43:43Z", **FIXED_BUG), 0, CLOSED_MARKER),
    ("an update demand on a bug closed by a released fix gets the redirect", dict(body="please update patches to 6.83.0", **FIXED_BUG), 0, CLOSED_MARKER),
    ("a crash report on a bug closed without a commit gets the redirect", dict(body=CRASHING, closing=["-"], labels=("bug",)), 0, CLOSED_MARKER),
    ("a crash report on a closed request gets the redirect", dict(body=CRASHING, closing=[RELEASED_SHA]), 0, CLOSED_MARKER),
    ("a failing timeline lookup fails the job", dict(body=ETA, closing=[UNRELEASED_SHA], timeline_fails=True), 1, None),
    ("a failing tag lookup fails the job", dict(body=ETA, closing=[UNRELEASED_SHA], tag_fails=True), 128, None),
]
for name, kwargs, code, marker in REPLY_CASES:
    status, posted, _ = run_reply(**kwargs)
    ok = status == code and (len(posted) == 1 and posted[0].startswith(marker + "\n") if marker else not posted)
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} reply job: {name}")

status, posted, calls = run_reply("any update?", state="open")
ok = status == 0 and len(posted) == 1 and posted[0].startswith(PING_MARKER) and not any("timeline" in " ".join(call) for call in calls)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} reply job: an open issue never reads the timeline")

ISSUE_EVENT = {"state": "open", "user": {"login": "reporter"}, "labels": [{"name": "needs info"}]}
COMMENT_EVENT = {"user": {"login": "reporter", "type": "User"}, "author_association": "CONTRIBUTOR"}
ok = answers_needs_info(ISSUE_EVENT, COMMENT_EVENT)
record(ok)
print(f"{'ok  ' if ok else 'FAIL'} needs info: a contributor-association author counts as the reporter")

FAKE_DISCUSSION_GH = r'''#!/usr/bin/env python3
import json, os, subprocess, sys

args = sys.argv[1:]
state = json.load(open(os.environ["FAKE_STATE"]))
state["calls"].append(args)
fields = {}
for flag, value in zip(args, args[1:]):
    if flag in ("-f", "-F") and "=" in value:
        key, _, raw = value.partition("=")
        fields.setdefault(key, []).append(open(raw[1:]).read() if raw.startswith("@") else raw)
operation = fields["query"][0].split("(")[0].split()[-1] if args[:2] == ["api", "graphql"] else ""
if operation == "Discussion":
    response = {"data": {"repository": {"discussion": {
        "number": 7, "title": "Something", "closed": False, "url": "", "createdAt": "2026-10-01T00:00:00Z",
        "author": {"login": "asker"}, "labels": {"nodes": [{"name": label} for label in state["labels"]]},
        "comments": {"nodes": [{"author": {"login": "github-actions"}, "body": body, "createdAt": "", "url": "c"} for body in state["bot_comments"]]}}}}}
elif operation == "DiscussionId":
    response = {"data": {"repository": {"discussion": {"id": "D7"}}}}
elif operation == "LabelId":
    response = {"data": {"repository": {"label": {"id": "L:" + fields["label"][0]}}}}
elif operation == "RemoveLabels":
    state["labels"] = [label for label in state["labels"] if "L:" + label not in fields["labels[]"]]
    response = {"data": {}}
elif operation == "AddComment":
    state["posted"].append(fields["body"][0])
    response = {"data": {"addDiscussionComment": {"comment": {"url": "c"}}}}
else:
    sys.exit(1)
json.dump(state, open(os.environ["FAKE_STATE"], "w"))
if "--jq" in args:
    done = subprocess.run(["jq", "-r", args[args.index("--jq") + 1]], input=json.dumps(response), capture_output=True, text=True)
    sys.stdout.write(done.stdout)
    sys.exit(done.returncode)
if "--silent" not in args:
    print(json.dumps(response))
'''


def request_script():
    lines = WORKFLOW.read_text().splitlines()
    start = lines.index("  request:")
    begin = next(i for i in range(start, len(lines)) if lines[i].strip() == "run: |") + 1
    end = next((i for i in range(begin, len(lines)) if lines[i] and not lines[i].startswith(" " * 10)), len(lines))
    return "\n".join(line[10:] for line in lines[begin:end])


def run_request(body, labels=("app request",), commenter="asker", bot_comments=()):
    event = {
        "discussion": {"number": 7, "state": "open", "title": "Something", "created_at": "2026-10-01T00:00:00Z",
                       "user": {"login": "asker"}, "category": {"slug": "app-requests"}},
        "comment": {"user": {"login": commenter, "type": "User"}, "author_association": "NONE", "body": body, "created_at": "2026-10-19T12:37:46Z"},
    }
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        (temp / "bin").mkdir()
        (temp / "bin" / "gh").write_text(FAKE_DISCUSSION_GH)
        (temp / "bin" / "gh").chmod(0o755)
        (temp / "state.json").write_text(json.dumps({"calls": [], "posted": [], "labels": list(labels), "bot_comments": list(bot_comments)}))
        (temp / "event.json").write_text(json.dumps(event))
        env = {"PATH": f"{temp / 'bin'}:{os.environ['PATH']}", "FAKE_STATE": str(temp / "state.json"), "GITHUB_EVENT_PATH": str(temp / "event.json"),
               "RUNNER_TEMP": str(temp), "NUMBER": "7", "REPO": "hxreborn/morphe-patches"}
        done = subprocess.run(["bash", "-eo", "pipefail", "-c", request_script()], env=env, cwd=HERE.parent.parent, capture_output=True, text=True)
        final = json.loads((temp / "state.json").read_text())
        return done.returncode, final["posted"], final["labels"]


REQUEST_CASES = [
    ("a status ping on an open request gets the ping reply", dict(body="any update?"), 0, True, ["app request"]),
    ("a second ping gets no reply", dict(body="any update?", bot_comments=[PING_MARKER + "\nx"]), 0, False, ["app request"]),
    ("thanks gets no reply", dict(body="Thanks for looking into it"), 0, False, ["app request"]),
    ("the requester's answer removes needs info", dict(body="here is the package", labels=("app request", "needs info")), 0, False, ["app request"]),
    ("someone else's comment keeps needs info", dict(body="here is the package", labels=("app request", "needs info"), commenter="other"), 0, False, ["app request", "needs info"]),
    ("a request marked bot-wrong is left alone", dict(body="any update?", labels=("app request", "bot-wrong")), 0, False, ["app request", "bot-wrong"]),
]
for name, kwargs, code, pinged, labels in REQUEST_CASES:
    status, posted, left = run_request(**kwargs)
    ok = status == code and (len(posted) == 1 and posted[0].startswith(PING_MARKER + "\n") if pinged else not posted) and left == labels
    record(ok)
    print(f"{'ok  ' if ok else 'FAIL'} request job: {name}")

print(f"\n{checks - failures}/{checks} passed")
sys.exit(1 if failures else 0)
