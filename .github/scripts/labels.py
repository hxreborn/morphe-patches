import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from urllib.parse import quote

from versions import targets

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "actions" / "triage"))
from fields import field, parse_fields

PRIORITIES = {"P0", "P1", "P2", "P3"}
TEMPORARY = {"needs info", "in progress", "in pre-release", "stale"}
UNASSESSED = {"needs info", "incomplete", "untargeted version", "upstream", "other bundle", "server-side"}
REGRESSION_ANSWER = "Yes, with an older patches release on the same app version"
STALE_EXEMPT = {"no-stale", "P0", "P1", "confirmed", "in progress", "in pre-release"}


def label_names(item):
    return {label["name"] for label in item.get("labels", [])}


def reported_regression(item, patches_list):
    labels = label_names(item)
    if item.get("pull_request") or item.get("user", {}).get("type") == "Bot" or labels & UNASSESSED:
        return None
    if "bug" not in labels:
        return None
    fields = parse_fields(item.get("body") or "")
    reported = field(fields, "App and version")
    supported = targets(patches_list)
    app = next((name for name in sorted(supported, key=len, reverse=True)
                if reported.casefold().startswith(name.casefold() + " ")), None)
    if not app:
        return None
    version = field(fields, "Other version") or reported[len(app):].strip()
    if version.casefold().removeprefix("v") not in {v.casefold().removeprefix("v") for v in supported[app]}:
        return None
    if field(fields, "Does the unpatched app do the same thing") == "The unpatched app fails the same way":
        return None
    answer = field(fields, "Did this work before")
    return answer == REGRESSION_ANSWER if answer else None


def automatic_changes(item, regression, timeline):
    labels = label_names(item)
    if "bot-wrong" in labels:
        return set(), set()
    if item["state"] == "closed":
        return set(), labels & TEMPORARY
    added, removed = set(), set()
    priorities = labels & PRIORITIES
    if len(priorities) > 1:
        chosen = min(priorities)
        for event in reversed(timeline):
            if event.get("event") == "labeled" and event.get("label", {}).get("name") in priorities:
                chosen = event["label"]["name"]
                break
        removed |= priorities - {chosen}
    if regression is not None:
        events = [event for event in timeline if event.get("event") in {"labeled", "unlabeled"}
                  and event.get("label", {}).get("name") == "regression"]
        manual = any((event.get("actor") or {}).get("type") != "Bot" for event in events)
        if not manual:
            if regression:
                added |= {"regression"} - labels
            else:
                removed |= {"regression"} & labels
    if ((labels | added) - removed) & STALE_EXEMPT:
        removed |= labels & {"stale"}
    return added, removed


def api(repo, path, *args, paginated=False):
    command = ["gh", "api", f"repos/{repo}/{path}", *args]
    if paginated:
        command += ["--paginate", "--slurp"]
    output = subprocess.run(command, check=True, capture_output=True, text=True).stdout
    result = json.loads(output) if output.strip() else None
    return [item for page in result for item in page] if paginated else result


def sync_labels(repo, catalog):
    existing = {label["name"]: label for label in api(repo, "labels?per_page=100", paginated=True)}
    for name, definition in catalog["labels"].items():
        current = existing.get(name)
        if (current and current["color"].lower() == definition["color"].lower()
                and current["description"] == definition["description"]):
            continue
        path = "labels/" + quote(name, safe="") if current else "labels"
        args = ["--method", "PATCH" if current else "POST", "-f", f'name={name}',
                "-f", f'color={definition["color"]}', "-f", f'description={definition["description"]}']
        api(repo, path, *args)
        print(f"Updated label: {name}")
    for name in catalog["retired"]:
        if name in existing:
            api(repo, "labels/" + quote(name, safe=""), "--method", "DELETE")
            print(f"Removed label: {name}")


def reconcile_issue(repo, item, patches_list):
    if "bot-wrong" in label_names(item):
        return
    regression = reported_regression(item, patches_list)
    timeline = []
    if item["state"] == "open" and (regression is not None or len(label_names(item) & PRIORITIES) > 1):
        timeline = api(repo, f'issues/{item["number"]}/timeline?per_page=100', paginated=True)
    added, removed = automatic_changes(item, regression, timeline)
    if not added and not removed:
        return
    number = item["number"]
    if added:
        args = ["--method", "POST"]
        for label in sorted(added):
            args.extend(("-f", f"labels[]={label}"))
        api(repo, f"issues/{number}/labels", *args)
    for label in sorted(removed):
        try:
            api(repo, f"issues/{number}/labels/{quote(label, safe='')}", "--method", "DELETE")
        except subprocess.CalledProcessError:
            remaining = api(repo, f"issues/{number}/labels?per_page=100", paginated=True)
            if any(item["name"] == label for item in remaining):
                raise
    print(f"#{number}: added {sorted(added)}, removed {sorted(removed)}")


def reconcile_discussions(repo):
    command = ["bash", ".github/actions/triage/discussion.sh"]
    env = {**os.environ, "REPO": repo}
    output = subprocess.run([*command, "list", "app-requests"], env=env, check=True, capture_output=True, text=True)
    for item in json.loads(output.stdout):
        if item["state"] != "CLOSED" or "bot-wrong" in label_names(item):
            continue
        removed = label_names(item) & TEMPORARY
        if removed:
            subprocess.run([*command, "remove-labels", str(item["number"]), *sorted(removed)], env=env, check=True)
            print(f'Discussion #{item["number"]}: removed {sorted(removed)}')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("sync", "event", "sweep"))
    parser.add_argument("--catalog", type=Path, default=Path(".github/labels.json"))
    parser.add_argument("--patches-list", type=Path, default=Path("patches-list.json"))
    args = parser.parse_args()
    repo = os.environ["GITHUB_REPOSITORY"]
    if args.command == "sync":
        sync_labels(repo, json.loads(args.catalog.read_text()))
        return
    patches_list = json.loads(args.patches_list.read_text())
    if args.command == "event":
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        number = (event.get("issue") or event.get("pull_request"))["number"]
        reconcile_issue(repo, api(repo, f"issues/{number}"), patches_list)
    else:
        for item in api(repo, "issues?state=all&per_page=100", paginated=True):
            reconcile_issue(repo, item, patches_list)
        reconcile_discussions(repo)


if __name__ == "__main__":
    main()
