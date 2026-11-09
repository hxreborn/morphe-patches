import json
import subprocess
from pathlib import Path
from types import SimpleNamespace

import labels
from issue_comment import closed_issue_reply, timestamp

PATCHES = {"patches": [{"compatiblePackages": [
    {"name": "One", "targets": [{"version": "1.0"}]},
    {"name": "Two", "targets": [{"version": "2.0"}]},
]}]}


def issue(*names, body="", state="open", user_type="User", **extra):
    return {"number": 9, "state": state, "body": body, "labels": [{"name": name} for name in names],
            "user": {"type": user_type}, **extra}


def report(symptom, previous="I have not checked", app="One 1.0"):
    return (f"### App and version\n\n{app}\n\n### What does not work\n\n{symptom}\n\n"
            f"### Did this work before\n\n{previous}")


def event(name, action="labeled", actor_type="User"):
    return {"event": action, "label": {"name": name}, "actor": {"type": actor_type}}


assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", previous=labels.REGRESSION_ANSWER)), PATCHES) is True
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", previous=labels.REGRESSION_ANSWER, app="Two 2.0")), PATCHES) is True
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", previous="No, it has never worked for me")), PATCHES) is False
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", app="One 9.0")), PATCHES) is None
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", app="Unknown 1.0")), PATCHES) is None
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect", app="One 10")), PATCHES) is None
assert labels.reported_regression(issue("bug", "untargeted version", body=report("The patch has no visible effect")), PATCHES) is None
assert labels.reported_regression(issue("bug", "needs info", body=report("The patch has no visible effect")), PATCHES) is None
assert labels.reported_regression(issue("app-update", user_type="Bot"), PATCHES) is None
assert labels.reported_regression(issue("bug", pull_request={"url": "example"}), PATCHES) is None
assert labels.reported_regression(issue("bug", body=report("The patch has no visible effect") + "\n\n### Does the unpatched app do the same thing\n\nThe unpatched app fails the same way"), PATCHES) is None
print("Per-app regression evidence passes")

for names, body in [(("bug",), report("The app will not open at all")),
                    (("bug",), report("The app opens, then crashes")),
                    (("bug",), report("The patch has no visible effect", previous=labels.REGRESSION_ANSWER)),
                    (("enhancement",), "### Priority\n\nP0\n\nurgent"),
                    (("app-update",), "Please update this app")]:
    item = issue(*names, body=body)
    added, removed = labels.automatic_changes(item, labels.reported_regression(item, PATCHES), [])
    assert not (added | removed) & labels.PRIORITIES
for priority in labels.PRIORITIES:
    assert labels.automatic_changes(issue(priority), None, []) == (set(), set())
assert labels.automatic_changes(issue("P1", "P3"), None, [event("P3"), event("P1")]) == (set(), {"P3"})
assert labels.automatic_changes(issue("P1", "P3"), None, [event("P1"), event("P3")]) == (set(), {"P1"})
assert labels.automatic_changes(issue("P0", "P1"), None, [event("P1"), event("P0")]) == (set(), {"P1"})
assert labels.automatic_changes(issue("P3"), None, [event("P1", actor_type="Bot"), event("P3"), event("P1", "unlabeled", "Bot")]) == (set(), set())
assert labels.automatic_changes(issue("P1"), True, [event("regression", "unlabeled")]) == (set(), set())
assert labels.automatic_changes(issue("P1", "regression"), False, [event("regression", actor_type="Bot")]) == (set(), {"regression"})
assert labels.automatic_changes(issue("confirmed", "P1"), None, []) == (set(), set())
assert labels.automatic_changes(issue("stale", "no-stale"), None, []) == (set(), {"stale"})
assert labels.automatic_changes(issue("stale", "confirmed"), None, []) == (set(), {"stale"})
assert labels.automatic_changes(issue("stale", "P1"), None, []) == (set(), {"stale"})
assert labels.automatic_changes(issue("bot-wrong", "needs info", state="closed"), None, []) == (set(), set())
assert labels.automatic_changes(issue("bug", "P1", "incomplete", "confirmed", "no-stale", *labels.TEMPORARY, state="closed"), None, []) == (set(), labels.TEMPORARY)
print("Manual overrides and closure cleanup pass")

calls = []
existing = [{"name": "bug", "color": "d73a4a", "description": "Old description"},
            {"name": "dependencies", "color": "0366d6", "description": "Old dependency label"},
            {"name": "custom", "color": "ffffff", "description": "Keep this label"}]


def fake_api(repo, path, *args, paginated=False):
    assert repo == "example/repo"
    if path == "labels?per_page=100":
        assert paginated
        return existing
    calls.append((path, args))
    return None


labels.api = fake_api
catalog = {"labels": {"bug": {"color": "D73A4A", "description": "New description"},
                      "P0": {"color": "B60205", "description": "Urgent"}},
           "retired": ["dependencies", "missing"]}
labels.sync_labels("example/repo", catalog)
assert [(path, args[1]) for path, args in calls] == [("labels/bug", "PATCH"), ("labels", "POST"), ("labels/dependencies", "DELETE")]
existing[:] = [{"name": name, **definition} for name, definition in catalog["labels"].items()]
calls.clear()
labels.sync_labels("example/repo", catalog)
assert calls == []
print("Label synchronization and idempotence pass")


def live_api(repo, path, *args, paginated=False):
    assert repo == "example/repo"
    if path == "issues/9/timeline?per_page=100":
        assert paginated
        return []
    calls.append((path, args))
    return None


labels.api = live_api
labels.reconcile_issue("example/repo", issue("bug", body=report("The patch has no visible effect", previous=labels.REGRESSION_ANSWER)), PATCHES)
assert calls == [("issues/9/labels", ("--method", "POST", "-f", "labels[]=regression"))]
calls.clear()
labels.reconcile_issue("example/repo", issue("in pre-release", "bug", state="closed"), PATCHES)
assert calls == [("issues/9/labels/in%20pre-release", ("--method", "DELETE"))]
print("Issue label mutations pass")

removal_error = subprocess.CalledProcessError(1, ["gh", "api"], stderr="Label removal failed")
remaining_labels = []
read_error = None


def failed_removal_api(repo, path, *args, paginated=False):
    assert repo == "example/repo"
    calls.append((path, args))
    if path == "issues/9/labels/in%20pre-release":
        assert args == ("--method", "DELETE")
        raise removal_error
    assert path == "issues/9/labels?per_page=100" and paginated
    if read_error:
        raise read_error
    return remaining_labels


labels.api = failed_removal_api
calls.clear()
remaining_labels[:] = [{"name": "bug"}, {"name": "P1"}]
labels.reconcile_issue("example/repo", issue("in pre-release", "bug", "P1", state="closed"), PATCHES)
assert calls == [("issues/9/labels/in%20pre-release", ("--method", "DELETE")),
                 ("issues/9/labels?per_page=100", ())]
remaining_labels.append({"name": "in pre-release"})
try:
    labels.reconcile_issue("example/repo", issue("in pre-release", state="closed"), PATCHES)
except subprocess.CalledProcessError as error:
    assert error is removal_error
else:
    raise AssertionError("Label removal failure was suppressed while the label was present")
read_error = subprocess.CalledProcessError(1, ["gh", "api"], stderr="Label lookup failed")
try:
    labels.reconcile_issue("example/repo", issue("in pre-release", state="closed"), PATCHES)
except subprocess.CalledProcessError as error:
    assert error is read_error
else:
    raise AssertionError("Label lookup failure was suppressed")
print("Concurrent label removal and API failure handling pass")

discussion_calls = []


def run_discussion(command, **kwargs):
    assert kwargs["env"]["REPO"] == "example/repo"
    if command[-2:] == ["list", "app-requests"]:
        items = [issue("needs info", state="CLOSED"), issue("needs info", state="OPEN"),
                 issue("bot-wrong", "needs info", state="CLOSED")]
        return SimpleNamespace(stdout=json.dumps(items))
    discussion_calls.append(command)


original_run = labels.subprocess.run
labels.subprocess.run = run_discussion
labels.reconcile_discussions("example/repo")
assert discussion_calls == [["bash", ".github/actions/triage/discussion.sh", "remove-labels", "9", "needs info"]]
labels.subprocess.run = original_run
print("Closed discussion cleanup passes")

catalog = json.loads(Path(".github/labels.json").read_text())
assert labels.PRIORITIES | {"regression", "confirmed", "no-stale"} <= set(catalog["labels"])
assert not set(catalog["labels"]) & set(catalog["retired"])
for definition in catalog["labels"].values():
    assert len(definition["color"]) == 6 and int(definition["color"], 16) >= 0
    assert 0 < len(definition["description"]) <= 100
print("Label catalog validation passes")

reply = closed_issue_reply(timestamp("2026-11-07T00:00:00Z"), keep_unlocked=True)
assert "will be locked" not in reply
print("Lock exemption reply passes")
