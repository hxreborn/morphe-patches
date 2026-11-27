#!/usr/bin/env python3
"""python3 .github/actions/triage/test_steps.py"""

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

import related_issues
from related_issues import TITLE_PREFIX

HERE = Path(__file__).resolve().parent
BOT = "github-actions[bot]"
PERSON = "hxreborn"
LONG = "Patching stops with an error dialog partway through and never writes an output file."
ACKNOWLEDGED = "### Acknowledgements\n\n- [X] I searched open and closed requests and this isn't a duplicate.\n"

PATCHES_LIST = {"version": "test", "patches": [{"compatiblePackages": [
    {"name": "Supported", "packageName": "com.supported.app", "targets": [{"version": "1.0.0"}]},
]}, {"compatiblePackages": [
    {"name": "Supported", "packageName": "com.supported.app", "targets": [{"version": "1.0.0"}]},
]}]}

REGISTRY = {"bundles": [
    {"repo": "hxreborn/morphe-patches", "name": "Hxreborn", "targetApps": ["com.supported.app"]},
    {"repo": "other/patches", "name": "Other", "targetApps": ["com.other.app"]},
]}

FAKE_GH = r'''#!/usr/bin/env python3
import json, os, subprocess, sys

path = os.environ["FAKE_STATE"]
state = json.load(open(path))
args = sys.argv[1:]
state["calls"].append(args)
json.dump(state, open(path, "w"))
if os.environ.get("FAIL_ON") and os.environ["FAIL_ON"] in " ".join(args):
    sys.exit(1)


def opt(name):
    return args[args.index(name) + 1] if name in args else None


def emit(value):
    if opt("--jq"):
        done = subprocess.run(["jq", "-r", opt("--jq")], input=json.dumps(value), capture_output=True, text=True)
        sys.stdout.write(done.stdout)
        sys.stderr.write(done.stderr)
        sys.exit(done.returncode)
    print(json.dumps(value))


issues = state["issues"]


def discussion(n, i):
    return {"number": int(n), "title": i["title"], "body": i["body"], "url": f"https://github.com/x/y/discussions/{n}",
            "createdAt": "2026-01-01T00:00:00Z", "updatedAt": i.get("updatedAt", "2026-01-01T00:00:00Z"),
            "state": i.get("live_state", i["state"]), "author": {"login": i.get("author", "requester")},
            "labels": [{"name": label} for label in i["labels"]],
            "comments": [{"author": {"login": "github-actions" if c["user"]["login"] == "github-actions[bot]" else c["user"]["login"]},
                          "body": c["body"], "createdAt": "2026-01-01T00:00:00Z", "url": ""} for c in i["comments"]]}


if args[:2] == ["api", "graphql"]:
    fields = {}
    for flag, value in zip(args, args[1:]):
        if flag in ("-f", "-F") and "=" in value:
            key, _, raw = value.partition("=")
            fields.setdefault(key, []).append(open(raw[1:]).read() if raw.startswith("@") else raw)
    operation = fields["query"][0].split("(")[0].split()[-1]
    number = fields.get("number", fields.get("id", ["D0"]))[0].lstrip("D")
    if operation == "CategoryId":
        emit({"data": {"repository": {"discussionCategory": {"id": "C1"}}}})
    elif operation == "RequestList":
        emit({"data": {"repository": {"discussions": {"pageInfo": {"hasNextPage": False, "endCursor": None},
              "nodes": [{**discussion(n, i), "closed": i["state"] == "CLOSED", "authorAssociation": i.get("association", "NONE"),
                         "labels": {"nodes": [{"name": label} for label in i["labels"]]},
                         "comments": {"nodes": discussion(n, i)["comments"]}} for n, i in issues.items()]}}}})
    elif operation == "DiscussionId":
        emit({"data": {"repository": {"discussion": {"id": f"D{number}"}}}})
    elif operation == "Discussion":
        i = issues[number]
        emit({"data": {"repository": {"discussion": {**discussion(number, i), "closed": i.get("live_state", i["state"]) == "CLOSED",
              "labels": {"nodes": [{"name": label} for label in i["labels"]]}, "comments": {"nodes": discussion(number, i)["comments"]}}}}})
    elif operation == "LabelId":
        emit({"data": {"repository": {"label": {"id": "L:" + fields["label"][0]}}}})
    else:
        issue = issues[number]
        if operation == "AddComment":
            issue["comments"].append({"user": {"login": "github-actions[bot]"}, "body": fields["body"][0]})
        elif operation == "Retitle":
            issue["title"] = fields["title"][0]
        elif operation == "AddLabels":
            issue["labels"] += [label[2:] for label in fields["labels[]"] if label[2:] not in issue["labels"]]
        elif operation == "RemoveLabels":
            issue["labels"] = [label for label in issue["labels"] if "L:" + label not in fields["labels[]"]]
        elif operation == "Close":
            issue["state"] = "CLOSED"
            issue["closed_as"] = fields["reason"][0]
        elif operation == "Lock" and "lockReason" in fields["query"][0]:
            sys.stderr.write("gh: You cannot specify a reason when locking a discussion.\n")
            sys.exit(1)
        elif operation == "Lock":
            issue["locked"] = True
        state["mutations"].append(f"{operation} {number}")
        json.dump(state, open(path, "w"))
        emit({"data": {}})
elif args[:2] == ["issue", "list"]:
    emit([{"number": int(n), "title": i["title"], "body": i["body"], "state": i["state"],
           "labels": [{"name": label} for label in i["labels"]], "comments": [],
           "author": {"login": i.get("author", "requester")}} for n, i in issues.items()])
elif args[0] == "api" and args[1].startswith("users/"):
    emit(state["events"].get(args[1].split("/")[1], []))
elif args[0] == "api":
    _, _, _, _, number, *kind = args[1].split("/")
    if kind:
        emit(issues[number][kind[0]])
    else:
        i = issues[number]
        emit({**i, "number": int(number), "state": i["state"].lower(),
              "labels": [{"name": label} for label in i["labels"]]})
elif args[:2] == ["search", "issues"]:
    fields = (opt("--json") or "").split(",")
    if opt("--match") != "title" or fields not in (["number", "title", "state"], ["number", "title"]) \
            or (args[-2] != "--" and not opt("--jq")):
        sys.exit(1)
    query = args[-1 if args[-2] == "--" else 2].lower().split()
    emit([{key: {"number": int(n), "title": i["title"], "state": i["state"].lower()}[key] for key in fields}
          for n, i in issues.items() if opt("--label") in i["labels"]
          and all(word in i["title"].lower() for word in query)])
elif args[:2] == ["issue", "view"]:
    emit({"state": issues[args[2]].get("live_state", issues[args[2]]["state"])})
else:
    issue = issues[args[2]]
    if args[1] == "comment":
        issue["comments"].append({"user": {"login": "github-actions[bot]"}, "body": open(opt("--body-file")).read()})
    elif args[1] == "close":
        issue["state"] = "CLOSED"
    elif args[1] == "reopen":
        issue["state"] = "OPEN"
    elif args[1] == "edit" and opt("--title"):
        issue["title"] = opt("--title")
    elif args[1] == "edit" and opt("--add-label"):
        issue["labels"] += [label for label in opt("--add-label").split(",") if label not in issue["labels"]]
    elif args[1] == "edit" and opt("--remove-label"):
        issue["labels"].remove(opt("--remove-label"))
    json.dump(state, open(path, "w"))
'''

FAKE_CURL = r'''#!/usr/bin/env bash
[ -n "$FAKE_REGISTRY" ] || exit 22
while [ $# -gt 0 ]; do [ "$1" = -o ] && cp "$FAKE_REGISTRY" "$2"; shift; done
'''


def step(name):
    lines = (HERE / "action.yml").read_text().splitlines()
    start = lines.index(f"    - name: {name}")
    begin = next(i for i in range(start, len(lines)) if lines[i].strip() == "run: |") + 1
    end = next((i for i in range(begin, len(lines)) if lines[i].startswith("    - name:")), len(lines))
    return "\n".join(line[8:] for line in lines[begin:end]).replace("${{ github.action_path }}", str(HERE))


def issue(title, body, labels=("app request",), state="OPEN", comments=(), timeline=()):
    return {"title": title, "body": body, "labels": list(labels), "state": state,
            "comments": [{"user": {"login": BOT}, "body": c} for c in comments], "timeline": list(timeline)}


def request(app, package):
    return f"### App\n\n{app}\n\n### Package name\n\n{package}\n\n### Requested features\n\nUnlock premium\n\n{ACKNOWLEDGED}"


def labeled(label, actor):
    return {"event": "labeled", "label": {"name": label}, "actor": {"login": actor}}


def event(kind, actor):
    return {"event": kind, "actor": {"login": actor}}


def opened(repo, number, title):
    return {"type": "IssuesEvent", "repo": {"name": repo},
            "payload": {"action": "opened", "issue": {"number": number, "title": title, "body": ""}}}


def run(issues, steps, issue_number="", registry=True, fail_on="", event_issue=None, action="edited", events=None,
        association="NONE", tagged_lists=None, preview=False):
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        (temp / "bin").mkdir()
        (temp / "runner").mkdir()
        for name, text in (("gh", FAKE_GH), ("curl", FAKE_CURL)):
            (temp / "bin" / name).write_text(text)
            (temp / "bin" / name).chmod(0o755)
        git = ["git", "-c", "user.name=t", "-c", "user.email=t@t", "-c", "core.hooksPath=/dev/null",
               "-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false", "-C", str(temp)]
        for tag, patches_list in (tagged_lists or {}).items():
            if not (temp / ".git").exists():
                subprocess.run([*git, "init", "-q"], check=True)
            (temp / "patches-list.json").write_text(json.dumps(patches_list))
            subprocess.run([*git, "add", "patches-list.json"], check=True)
            subprocess.run([*git, "commit", "-qm", tag], check=True)
            subprocess.run([*git, "tag", tag], check=True)
        (temp / "patches-list.json").write_text(json.dumps(PATCHES_LIST))
        (temp / "registry.json").write_text(json.dumps(REGISTRY))
        (temp / "state.json").write_text(json.dumps({"issues": issues, "calls": [], "mutations": [], "events": events or {}}))
        event_issue = event_issue or {}
        (temp / "event.json").write_text(json.dumps({"issue": {
            **event_issue, "labels": [{"name": label} for label in event_issue.get("labels", [])]}}))
        env = {
            "PATH": f"{temp / 'bin'}:{os.environ['PATH']}", "HOME": str(temp), "FAKE_STATE": str(temp / "state.json"),
            "FAKE_REGISTRY": str(temp / "registry.json") if registry else "", "FAIL_ON": fail_on,
            "GH_TOKEN": "fake", "REPO": "hxreborn/morphe-patches", "GITHUB_REPOSITORY": "hxreborn/morphe-patches",
            "ISSUE": str(issue_number), "NUMBER": str(issue_number), "CATEGORY": "app-requests",
            "REGISTRY": "https://registry.invalid", "PATCHES_LIST": "patches-list.json",
            "DRY_RUN": "false", "RUNNER_TEMP": str(temp / "runner"), "GITHUB_EVENT_PATH": str(temp / "event.json"),
            "ACTION": action, "ASSOCIATION": association, "CLOSED": str(event_issue.get("state") == "closed").lower(),
            "PREVIEW_ISSUE": str(issue_number) if preview else "", "GITHUB_STEP_SUMMARY": str(temp / "summary.md"),
            "GITHUB_OUTPUT": str(temp / "output.txt"),
        }
        status = 0
        for name in steps:
            status = subprocess.run(["bash", "--noprofile", "--norc", "-eo", "pipefail", "-c", step(name)],
                                    cwd=temp, env=env, capture_output=True, text=True).returncode
            if status:
                break
        state = json.loads((temp / "state.json").read_text())
        mutations = [" ".join(c[:3]) for c in state["calls"] if c[0] == "issue" and c[1] in ("comment", "close", "reopen", "edit")]
        if preview:
            summary, output = temp / "summary.md", temp / "output.txt"
            return status, mutations + state["mutations"], summary.read_text() if summary.exists() else "", \
                output.read_text() if output.exists() else ""
        return status, state["issues"], mutations + state["mutations"]


SWEEP = ["Triage app requests"]
UPDATE = ["Triage app update requests"]
BUG = ["Check the report", "Act on the verdict"]
failures = 0
checks = 0


def check(ok, name):
    global failures, checks
    checks += 1
    failures += not ok
    print(f"{'ok  ' if ok else 'FAIL'} {name}")


def pair(**second):
    return {"10": issue("[App Request]: New", request("New", "com.new.app")),
            "11": {**issue("[App Request]: New again", request("New again", "com.new.app")), **second}}


# when
status, issues, mutations = run({**pair(), "14": issue("[App Request]: Other", request("Other", "com.other.app"))}, SWEEP)

# then
check(status == 0 and issues["11"]["state"] == "CLOSED" and issues["11"]["closed_as"] == "DUPLICATE"
      and issues["10"]["state"] == "OPEN" and "needs info" in issues["14"]["labels"] and len(issues["14"]["comments"]) == 1,
      "the sweep closes a later duplicate and flags a request another bundle covers")

# when
status, _, mutations = run(issues, SWEEP)

# then
check(status == 0 and mutations == [], "a second sweep changes nothing")

# given
_, reopened, _ = run(pair(), SWEEP)
reopened["11"]["state"] = "OPEN"

# when
status, issues, mutations = run(reopened, SWEEP)

# then
check(status == 0 and issues["11"]["state"] == "OPEN" and mutations == [],
      "a request reopened after the bot closed it for the same reason is not closed again")

# given
gone = pair()
gone["10"]["live_state"] = "CLOSED"

# when
status, issues, mutations = run(gone, SWEEP)

# then
check(status == 0 and issues["11"]["state"] == "OPEN" and mutations == [],
      "an original closed during the sweep is not used as the duplicate target")

# given
titles = {"12": issue("[App Request]: my app", request("Mine", "_No response_")),
          "13": issue("", request("Other", "_No response_"))}

# when
status, issues, _ = run(titles, SWEEP)

# then
check(status == 0 and issues["12"]["title"] == "[App Request]: my app" and issues["13"]["title"] == "",
      "the sweep leaves titles alone")

# when
status, issues, _ = run(titles, SWEEP, issue_number=12, action="created")
status_empty, issues_empty, _ = run(titles, SWEEP, issue_number=13, action="created")

# then
check(status == 0 and issues["12"]["title"] == "my app" and status_empty == 0 and issues_empty["13"]["title"] == "Other",
      "a new request loses the form prefix and an empty title takes the app name")

# when
status, issues, mutations = run(pair(), SWEEP, issue_number=11, action="created", fail_on="updateDiscussion")

# then
check(status == 0 and issues["11"]["title"] == "[App Request]: New again" and issues["11"]["state"] == "CLOSED",
      "a new request that cannot be retitled is still closed as a duplicate")

# when
status, issues, mutations = run(pair(), SWEEP)

# then
check(status == 0 and issues["11"].get("locked") and not issues["10"].get("locked"),
      "the sweep locks the duplicate it closes")

# when
status, issues, _ = run({**pair(), "14": issue("[App Request]: Other", request("Other", "com.other.app"))}, SWEEP,
                        fail_on="id=D11")

# then
check(status != 0 and issues["11"]["state"] == "OPEN" and "needs info" in issues["14"]["labels"],
      "a failure on one request does not stop the requests after it")

# given
flagged = {"14": issue("[App Request]: Other", request("Other", "com.other.app"), labels=("app request", "needs info"),
                       comments=["<!-- app-request-bot -->\nan earlier flag"], timeline=[labeled("needs info", BOT)])}

# when
status, issues, mutations = run(flagged, SWEEP, registry=False)

# then
check(status == 0 and mutations == [] and "needs info" in issues["14"]["labels"],
      "flagged requests are left alone while the registry is unavailable")

# given
_, issues, _ = run({"14": issue("[App Request]: Other", request("Other", "com.other.app"))}, SWEEP)
issues["14"]["labels"].remove("needs info")

# when
status, issues, mutations = run(issues, SWEEP)

# then
check(status == 0 and mutations == [] and "needs info" not in issues["14"]["labels"],
      "a label a person removed is not added back while the comment is unchanged")

# given
posted = {"15": {**issue("[App Request]: Musicolet", request("Musicolet", "_No response_")), "author": "poster"}}
elsewhere = {"poster": [opened("other/patches", 1099, "feat: new app [Musicolet]"),
                        opened("stranger/kodi", 3, "Musicolet support")]}

# when
status, issues, mutations = run(posted, SWEEP, events=elsewhere)

# then
check(status == 0 and "other bundle" in issues["15"]["labels"] and len(issues["15"]["comments"]) == 1
      and "[other/patches#1099](https://github.com/other/patches/issues/1099)" in issues["15"]["comments"][0]["body"]
      and "stranger/kodi" not in issues["15"]["comments"][0]["body"],
      "the sweep labels and links a request its author also filed in a listed bundle")

# when
status, issues, mutations = run(issues, SWEEP, events={})

# then
check(status == 0 and mutations == [] and "other bundle" in issues["15"]["labels"],
      "a cross-post that leaves the author's event feed changes nothing")

# when
status, issues, mutations = run(issues, SWEEP, fail_on="users/poster/events")

# then
check(status == 0 and mutations == [] and "other bundle" in issues["15"]["labels"],
      "a flagged request is left alone while its author's other requests cannot be read")



def requests_by(author, count):
    return {str(40 + i): {**issue(f"App {i}", request(f"App {i}", f"com.app{i}.reader")), "author": author}
            for i in range(count)}


# given
crowded = requests_by("busy", 4)
crowded["41"]["labels"].append("needs info")

# when
status, issues, mutations = run(crowded, SWEEP, issue_number=43, action="created")

# then
check(status == 0 and issues["43"]["state"] == "CLOSED" and issues["43"]["closed_as"] == "OUTDATED"
      and len(issues["43"]["comments"]) == 1
      and "[#41](https://redirect.github.com/hxreborn/morphe-patches/discussions/41)" in issues["43"]["comments"][0]["body"]
      and all(issues[n]["state"] == "OPEN" for n in ("40", "41", "42")),
      "a fourth open request from one person is closed with one reply listing the other three")

# when
status, issues, mutations = run(issues, SWEEP, issue_number=43, action="created")

# then
check(status == 0 and mutations == [], "a request closed for the limit gets no second reply")

# when
status, issues, mutations = run(requests_by("busy", 4), SWEEP)

# then
check(status == 0 and mutations == [], "the sweep does not close requests already over the limit")

# when
status, issues, mutations = run(requests_by(PERSON, 4), SWEEP, issue_number=43, action="created", association="OWNER")

# then
check(status == 0 and issues["43"]["state"] == "OPEN" and mutations == [], "the owner is not limited")

# given
formless = {"50": issue("Pixlr", "Remove ads and make a clean homepage.", labels=())}

# when
status, issues, mutations = run(formless, SWEEP)

# then
check(status == 0 and mutations == ["AddLabels 50", "AddComment 50", "Close 50", "Lock 50"]
      and issues["50"]["labels"] == ["app request"] and issues["50"]["closed_as"] == "OUTDATED",
      "the sweep labels, answers, closes and locks a request opened without the form")

# when
status, issues, mutations = run(formless, SWEEP, fail_on="lockLockable")

# then
check(status != 0 and issues["50"]["state"] == "CLOSED" and not issues["50"].get("locked"),
      "a failed lock fails the run and leaves the request closed")

# when
status, issues, mutations = run({"50": {**formless["50"], "association": "OWNER"}}, SWEEP)

# then
check(status == 0 and mutations == [] and issues["50"]["state"] == "OPEN", "the owner's announcement is left alone")

unknown_update = "### App\n\nUnknown\n\n### New version\n\n2.0\n"
update_payload = {"number": 30, "state": "open", "title": "[App Update]: Unknown 2.0", "body": unknown_update,
                  "labels": ["app-update"]}

# given
_, issues, _ = run({"30": issue("[App Update]: Unknown 2.0", unknown_update, labels=("app-update",))}, UPDATE,
                   issue_number=30, event_issue=update_payload)
issues["30"]["labels"].remove("needs info")

# when
status, issues, mutations = run(issues, UPDATE, issue_number=30, event_issue=update_payload)

# then
check(status == 0 and mutations == [] and issues["30"]["labels"] == ["app-update"],
      "an app update label a person removed is not added back while the comment is unchanged")

supported_failure = ("### App\n\nSupported\n\n### New version\n\n1.0.0\n\n"
                     "### Patching this version\n\nPatching works, the app misbehaves\n\n"
                     "### Error logs\n\nBackup does not restore saved data on the patched app.\n")
failure_payload = {"number": 30, "state": "open", "title": "[App Update]: Supported 1.0.0",
                   "body": supported_failure, "labels": ["app-update"]}
status, issues, mutations = run({"30": issue("[App Update]: Supported 1.0.0", supported_failure,
                                            labels=("app-update",))}, UPDATE,
                                issue_number=30, event_issue=failure_payload, action="opened")
check(status == 0 and issues["30"]["state"] == "OPEN" and issues["30"]["labels"] == ["app-update"]
      and mutations == ["issue comment 30"],
      "an already-targeted version with a reported failure gets no close or needs info label")

for actor in (BOT, PERSON):
    previous = "<!-- app-update-bot -->\nEarlier missing information"
    corrected_payload = {**failure_payload, "labels": ["app-update", "needs info"]}
    status, issues, mutations = run({"30": issue("[App Update]: Supported 1.0.0", supported_failure,
                                                labels=("app-update", "needs info"), comments=(previous,),
                                                timeline=[labeled("needs info", actor)])}, UPDATE,
                                    issue_number=30, event_issue=corrected_payload)
    expected_labels = ["app-update"] if actor == BOT else ["app-update", "needs info"]
    check(status == 0 and issues["30"]["state"] == "OPEN" and issues["30"]["labels"] == expected_labels,
          f"a corrected supported-version failure removes only obsolete bot labels, label actor={actor}")

bug_body = f"### App and version\n\nSupported 1.0.0\n\n### Bug description\n\n{LONG}\n"
short_body = "### App and version\n\nSupported 1.0.0\n\n### Bug description\n\nbroken\n"


def bug(labels, closer, body, timeline=()):
    issues = {"20": issue("[Bug]: x", body, labels=labels, state="CLOSED", timeline=[*timeline, event("closed", closer)])}
    return issues, {"number": 20, "state": "closed", "title": "[Bug]: x", "body": body, "labels": list(labels)}


# given
issues, payload = bug(("bug", "needs info"), PERSON, bug_body)

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and mutations == [] and issues["20"]["state"] == "CLOSED", "a report a person closed is left alone")

# given
issues, payload = bug(("bug", "needs info"), BOT, bug_body)

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and mutations == [], "a bot close that was neither triage nor stale is left alone")

# given
issues, payload = bug(("bug", "incomplete", "needs info"), BOT, bug_body,
                      timeline=[labeled("incomplete", BOT), labeled("needs info", PERSON)])

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and issues["20"]["state"] == "OPEN" and issues["20"]["labels"] == ["bug", "needs info"],
      "a fixed report the triage bot closed is reopened and keeps a person's label")

# given
issues, payload = bug(("bug", "needs info", "stale"), BOT, short_body, timeline=[labeled("needs info", BOT)])

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and issues["20"]["state"] == "CLOSED", "a stale-closed report that still lacks information is not reopened")

# given
issues, payload = bug(("bug", "needs info", "stale"), BOT, bug_body, timeline=[labeled("needs info", BOT)])

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and issues["20"]["state"] == "OPEN", "a stale-closed report fixed by its author is reopened")

blocked_body = f"{bug_body}\n### APK source\n\nhttps://softonic.com/app\n"

# given
issues, payload = bug(("bug", "incomplete"), BOT, blocked_body)
_, issues, _ = run(issues, BUG, issue_number=20, event_issue=payload)

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and mutations == [], "an unchanged report the triage bot closed gets no second close or label")

# given
issues, payload = bug(("bug", "stale", "incomplete"), BOT, short_body,
                      timeline=[event("closed", BOT), event("reopened", PERSON), labeled("stale", BOT)])

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and issues["20"]["state"] == "CLOSED",
      "a report the stale job closed after a person reopened it stays closed while it still fails")

# given
issues = {"20": issue("[Bug]: x", blocked_body, labels=("bug", "stale"), timeline=[labeled("stale", BOT)])}
payload = {"number": 20, "state": "open", "title": "[Bug]: x", "body": blocked_body, "labels": ["bug", "stale"]}

# when
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)

# then
check(status == 0 and issues["20"]["state"] == "CLOSED" and issues["20"]["labels"] == ["bug", "incomplete"],
      "a triage close removes the stale label so a later fix reopens the report")

runtime_body = ("### App and version\n\nSupported 1.0.0\n\n"
                "### What does not work\n\nThe patch has no visible effect\n\n"
                "### Steps to reproduce\n\nOpen connection settings and exclude a location from the list.\n\n"
                "### Expected behaviour\n\nConnections skip the selected location.\n\n"
                "### Actual behaviour\n\nConnections still use the selected location.\n\n"
                "### Error logs\n\nNone\n")
runtime_payload = {"number": 20, "state": "open", "title": "[Bug]: x", "body": runtime_body, "labels": ["bug"]}
status, issues, mutations = run({"20": issue("[Bug]: x", runtime_body, labels=("bug",))}, BUG,
                                issue_number=20, event_issue=runtime_payload, action="opened")
check(status == 0 and mutations == [] and issues["20"]["state"] == "OPEN"
      and issues["20"]["labels"] == ["bug"],
      "a documented non-crashing failure without logs gets no close or needs info label")

restore_title = "[Not working]: Supported: restore from Google drive not working"
restore_payload = {**runtime_payload, "title": restore_title}
restore_report = issue(restore_title, runtime_body, labels=("bug",))
earlier_backup = issue("[Not working]: Supported backup fails without GmsCore support", runtime_body, labels=("bug",))
status, issues, mutations = run({"19": earlier_backup, "20": restore_report}, BUG,
                                issue_number=20, event_issue=restore_payload, action="opened")
check(status == 0 and mutations == ["issue comment 20", "issue edit 20"] and issues["20"]["state"] == "OPEN"
      and issues["20"]["comments"][0]["body"].startswith("<!-- related-issues-bot -->")
      and "[#19](https://redirect.github.com/hxreborn/morphe-patches/issues/19)" in issues["20"]["comments"][0]["body"]
      and issues["20"]["labels"] == ["bug", "possible duplicate"],
      "a complete new report links an earlier report that names the same problem in other words and gets possible duplicate")

conventional_title = "bug(Supported): Restore from Google drive not working"
conventional_report = issue(conventional_title, runtime_body, labels=("bug",))
conventional_earlier = issue("bug(Supported): Backup fails without GmsCore support", runtime_body, labels=("bug",))
status, issues, mutations = run({"19": conventional_earlier, "20": conventional_report}, BUG,
                                issue_number=20, event_issue={**runtime_payload, "title": conventional_title}, action="opened")
check(status == 0 and mutations == ["issue comment 20", "issue edit 20"]
      and "[#19](https://redirect.github.com/hxreborn/morphe-patches/issues/19)" in issues["20"]["comments"][0]["body"],
      "a renamed report with a conventional title links an earlier report with one")

for title, rest in (("[Bug]: Restore fails", "Restore fails"), ("[Not working]: Supported: restore", "Supported: restore"),
                    ("bug: Restore fails", "Restore fails"), ("feat(Supported): Dark theme", "Dark theme"),
                    ("question: How to restore", "How to restore"), ("Restore fails", "Restore fails"),
                    ("bugfix: Restore fails", "bugfix: Restore fails")):
    check(TITLE_PREFIX.sub("", title) == rest, f"the related report search drops the prefix of {title!r}")

searched = []


def record_search(command, **options):
    searched.append(command[-1])
    return subprocess.CompletedProcess(command, 0, stdout="[]")


real_run = related_issues.subprocess.run
related_issues.subprocess.run = record_search
related_issues.suggestions({"number": 20, "title": "feat(Supported): Restore from Google drive", "body": ""},
                           "Supported", "bug", "hxreborn/morphe-patches")
related_issues.subprocess.run = real_run
check(searched and not any(word in query.split() for query in searched for word in ("feat", "bug", "question")),
      "the related report search never queries the title prefix")

earlier_crash = issue("[Not working]: Supported crashes on launch", runtime_body, labels=("bug",))
status, issues, mutations = run({"19": earlier_crash, "20": restore_report}, BUG,
                                issue_number=20, event_issue=restore_payload, action="opened")
check(status == 0 and mutations == [] and issues["20"]["labels"] == ["bug"],
      "a complete new report gets no comment and no possible duplicate label when no earlier report matches")

status, issues, mutations = run({"19": earlier_backup, "20": restore_report}, BUG,
                                issue_number=20, event_issue=restore_payload)
check(status == 0 and mutations == [], "an edited complete report gets no related reports comment")

flagged_title = "[Not working]: Supported broken"
flagged_report = issue(flagged_title, short_body, labels=("bug",))
flagged_payload = {"number": 20, "state": "open", "title": flagged_title, "body": short_body, "labels": ["bug"]}
status, issues, mutations = run({"19": issue("[Not working]: Supported crashes on launch", runtime_body, labels=("bug",)),
                                 "20": flagged_report}, BUG, issue_number=20, event_issue=flagged_payload, action="opened")
check(status == 0 and issues["20"]["labels"] == ["bug", "needs info", "possible duplicate"]
      and "These issues may be about the same problem:" in issues["20"]["comments"][0]["body"],
      "a flagged new report that gets the related reports list is labeled possible duplicate")

status, issues, mutations = run({"20": flagged_report}, BUG, issue_number=20, event_issue=flagged_payload, action="opened")
check(status == 0 and issues["20"]["labels"] == ["bug", "needs info"],
      "a flagged new report with no related reports is not labeled possible duplicate")

stale_label = {**restore_payload, "labels": ["bug", "possible duplicate"]}
related_comment = "<!-- related-issues-bot -->\nThese issues may be about the same problem:\n\n- [#19](https://x)"
for actor, earlier, expected in ((BOT, {}, ["bug"]), (PERSON, {}, ["bug", "possible duplicate"]),
                                 (BOT, {"19": earlier_backup}, ["bug", "possible duplicate"])):
    status, issues, mutations = run(
        {**earlier, "20": issue(restore_title, runtime_body, labels=("bug", "possible duplicate"), comments=(related_comment,),
                                timeline=[labeled("possible duplicate", actor)])},
        BUG, issue_number=20, event_issue=stale_label)
    check(status == 0 and issues["20"]["labels"] == expected,
          f"possible duplicate is removed only when the bot added it and no related report is left, "
          f"label actor={actor}, earlier reports={len(earlier)}")

answered_report = {**restore_report, "comments": [{"user": {"login": PERSON}, "body": "was GmsCore support enabled?"}]}
status, issues, mutations = run({"19": earlier_backup, "20": answered_report}, BUG,
                                issue_number=20, event_issue=restore_payload, action="opened")
check(status == 0 and mutations == [], "a new report someone already answered gets no related reports comment")

issues, payload = bug(("bug", "incomplete", "needs info"), BOT, runtime_body,
                      timeline=[labeled("incomplete", BOT), labeled("needs info", BOT)])
issues["20"]["comments"] = [{"user": {"login": BOT}, "body": "<!-- triage-bot -->\nEarlier log requirement"}]
status, issues, mutations = run(issues, BUG, issue_number=20, event_issue=payload)
check(status == 0 and issues["20"]["state"] == "OPEN" and issues["20"]["labels"] == ["bug"],
      "a non-crashing report closed for missing logs reopens and loses bot validation labels")

DEV_LIST = {"version": "1.1.0-dev.1", "patches": [{"compatiblePackages": [
    {"name": "Supported", "packageName": "com.supported.app", "targets": [{"version": "2.0.0"}]},
]}]}
dev_body = (f"### App and version\n\nSupported 2.0.0\n\n### Bug description\n\n{LONG}\n\n"
            "### Error logs\n\nApp: Supported\nSource: hxreborn\u2019s patches 1.1.0-dev.1\nManager: 2.2.0-dev.3\n")
dev_report = {"number": 20, "state": "open", "title": "[Bug]: x", "body": dev_body, "labels": ["bug"]}

# when
status, issues, mutations = run({"20": issue("[Bug]: x", dev_body, labels=("bug",))}, BUG, issue_number=20,
                                event_issue=dev_report, action="opened", tagged_lists={"v1.1.0-dev.1": DEV_LIST})

# then
check(status == 0 and mutations == [], "a report from a pre-release bundle is checked against that pre-release's targets")

# when
status, issues, mutations = run({"20": issue("[Bug]: x", dev_body, labels=("bug",))}, BUG, issue_number=20,
                                event_issue=dev_report, action="opened")

# then
check(status == 0 and "untargeted version" not in issues["20"]["labels"]
      and "App support could not be checked against patches" in issues["20"]["comments"][0]["body"],
      "a report naming a pre-release with no tag does not get a compatibility label")

stable_list = {**DEV_LIST, "version": "1.1.0"}
stable_body = f"### App and version\n\nSupported 2.0.0\n\n### Patches version\n\n1.1.0\n\n### Bug description\n\n{LONG}\n"
status, issues, mutations = run({"20": issue("Supported", stable_body, labels=("bug",))}, BUG,
                                issue_number=20, event_issue={**dev_report, "body": stable_body},
                                tagged_lists={"v1.1.0": stable_list})
check(status == 0 and mutations == [], "the form selects the exact stable release without a log")

conflicting = stable_body + "\n### Error logs\n\nSource: hxreborn\u2019s patches 1.1.0-dev.1\nManager: 2.2.0-dev.3\n"
status, issues, mutations = run({"20": issue("Supported", conflicting, labels=("bug",))}, BUG,
                                issue_number=20, event_issue={**dev_report, "body": conflicting},
                                tagged_lists={"v1.1.0-dev.1": DEV_LIST, "v1.1.0": stable_list})
check(status == 0 and "untargeted version" not in issues["20"]["labels"]
      and "more than one patches version" in issues["20"]["comments"][0]["body"],
      "a form and log disagreement leaves compatibility unverified")

PREVIEW = ["Load issue for preview"]
preview_body = (stable_body + "\n### Patch that breaks it\n\nSupported \u00b7 Sync library\n"
                "\n### Actual behaviour\n\nThe queue duplicates episodes on refresh.\n")
preview_issues = {
    "20": issue("Supported queue duplicates episodes", preview_body, labels=("bug",)),
    "16": issue("Supported queue duplicates on refresh", "", labels=("bug",), state="CLOSED"),
    "17": issue("Supported Sync library freezes on launch", "", labels=("bug",)),
    "18": issue("Supported queue duplicates episodes", "", labels=("enhancement",)),
    "21": issue("Supported queue duplicates episodes", "", labels=("bug",)),
}
status, mutations, summary, output = run(preview_issues, PREVIEW + BUG, issue_number=20, preview=True,
                                         tagged_lists={"v1.1.0": stable_list})
check(status == 0 and mutations == [] and output == "kind=bug\n"
      and "Manifest: reported 1.1.0" in summary and "[#16]" in summary
      and all(f"[#{n}]" not in summary for n in (17, 18, 20, 21)),
      "a passing bug preview uses the tagged manifest and lists only matching older bug reports")

bad_body = stable_body.replace(LONG, "none")
status, mutations, summary, output = run({"20": issue("Supported", bad_body, labels=("bug",))},
                                         PREVIEW + BUG, issue_number=20, preview=True,
                                         tagged_lists={"v1.1.0": stable_list})
check(status == 0 and mutations == [] and "Post comment" in summary and "Add labels: incomplete" in summary
      and "Close as not planned" in summary,
      "a bug preview shows the comment, label and close without a write")

bot_closed = issue("Supported", stable_body, labels=("bug", "incomplete"), state="CLOSED",
                   timeline=[event("closed", BOT), labeled("incomplete", BOT)])
status, mutations, summary, output = run({"20": bot_closed}, PREVIEW + BUG, issue_number=20, preview=True,
                                         tagged_lists={"v1.1.0": stable_list})
check(status == 0 and mutations == [] and "Reopen issue" in summary and "Remove label: incomplete" in summary,
      "a corrected closed report previews reopening and removal of the bot label")

unsupported = "### App\n\nUnknown Reader\n\n### Feature description\n\nAdd collection folders.\n"
status, mutations, summary, output = run({"20": issue("Unknown Reader", unsupported, labels=("enhancement",))},
                                         PREVIEW + ["Triage feature requests"], issue_number=20, preview=True)
check(status == 0 and mutations == [] and output == "kind=feature\n"
      and "Post comment" in summary and "Close as not planned" in summary,
      "a feature preview reports a proposed close without a write")

status, mutations, summary, output = run(preview_issues, PREVIEW, issue_number="0", preview=True)
check(status != 0 and mutations == [] and summary == "", "preview rejects nonpositive issue numbers")

print(f"\n{checks - failures}/{checks} passed")
sys.exit(1 if failures else 0)
