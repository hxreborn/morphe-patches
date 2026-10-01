from app_updates import compare

CASES = [
    ("2.7.9", "2.7.8", 1),
    ("2.7.8", "2.7.8.0", 0),
    ("2.4.0f", "2.4.0e", 1),
    ("2.4.0e", "2.4.0", 1),
    ("1.9", "1.9-beta5", 1),
    ("2.0-rc1", "1.9", 1),
    ("6.0", "6.0_arm64", 0),
    ("6.1", "6.0_arm64", 1),
    ("8.6_arm64", "8.6_x86_64", 0),
    ("8.6_arm64-v8a", "8.6", 0),
    ("8.7_x86_64", "8.6_arm64", 1),
    ("1.78.49", "Android V1.78.49", 0),
    ("Android V1.78.50", "1.78.49", 1),
    ("14.0.0 TV", "14.0.0", 0),
    ("26.05.20+2300", "26.05.20+2299", 1),
    ("p5.11.2", "p5.11.1", 1),
    ("4.0.03.0918.03", "4.0.02.0903.02", 1),
    ("5.1.7_20260914", "5.1.7_20260901", 1),
    ("407.0.0.178994", "406.1.0.170000", 1),
    ("32.30.0(1575420)", "32.30.0", 1),
    ("", "1.0", -1),
]

for a, b, expected in CASES:
    assert compare(a, b) == expected, (a, b, compare(a, b), expected)
    assert compare(b, a) == -expected, (b, a)
print(f"{len(CASES)} version comparisons pass")

import json
import app_updates

calls = []
issues = [{"number": 1, "state": "OPEN", "title": "[App Update]: Cx File Explorer 2.7.9.0"},
          {"number": 2, "state": "OPEN", "title": "[App Update]: Showly 3.71.0"},
          {"number": 3, "state": "CLOSED", "title": "[App Update]: Showly 3.72.0"},
          {"number": 4, "state": "OPEN", "title": "[App Update]: Audible 26.38.08"}]
app_updates.gh = lambda *args, **kw: calls.append(args) or (json.dumps(issues) if args[:2] == ("issue", "list") else "")
apps = {p: {"package": p, "name": n, "current": c, "current_version_codes": []} for p, n, c in [
    ("cx", "Cx File Explorer", "2.7.8"), ("showly", "Showly", "3.70.0"), ("audible", "Audible", "26.38.08"),
    ("new", "Newcomer", "1.0")]}
record = {"status": "update", "reported_by": ["play"], "confirmed_by": ["play"], "readiness": "ready"}
app_updates.sync_issues(apps, [{**record, "package": "cx", "candidate": "2.7.9"},
                               {**record, "package": "showly", "candidate": "3.72.0"},
                               {**record, "package": "audible", "candidate": None, "status": "current"},
                               {**record, "package": "new", "candidate": "1.1"}])
actions = [c[1:3] for c in calls if c[0] == "issue" and c[1] != "list"]
assert actions == [("edit", "1"), ("close", "2"), ("close", "4"),
                   ("create", "--title")], actions
assert any(c[3] == "[App Update]: Newcomer 1.1" for c in calls if c[1] == "create"), calls
print("issue sync: edit on equal version, close superseded and targeted, skip closed title, create new")
