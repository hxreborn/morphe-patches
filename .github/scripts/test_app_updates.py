import json
import tempfile
import zipfile
from pathlib import Path

import app_updates
from app_updates import compare

CASES = [
    ("2.7.9", "2.7.8", 1),
    ("2.7.8", "2.7.8.0", 0),
    ("2.4.0f", "2.4.0e", 1),
    ("2.4.0e", "2.4.0", 1),
    ("1.9", "1.9-beta5", 1),
    ("2.0", "2.0 beta", 1),
    ("2.0-rc1", "2.0.0-rc1", 0),
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

calls = []
issues = [{"number": 1, "state": "OPEN", "title": "[App Update]: Cx File Explorer 2.7.9.0"},
          {"number": 2, "state": "OPEN", "title": "[App Update]: Showly 3.71.0"},
          {"number": 3, "state": "CLOSED", "title": "[App Update]: Showly 3.72.0"},
          {"number": 4, "state": "OPEN", "title": "[App Update]: Audible 26.38.08"},
          {"number": 5, "state": "OPEN", "title": "[App Update]: MovieBox TV 1.2.0.0924"},
          {"number": 6, "state": "OPEN", "title": "[App Update]: MovieBox TV"},
          {"number": 7, "state": "OPEN", "title": "[App Update]: Newcomer 1.0.5", "author": {"is_bot": False}},
          {"number": 8, "state": "OPEN", "title": "[App Update]: Held 1.5", "labels": [{"name": "needs info"}]},
          {"number": 9, "state": "OPEN", "title": "[App Update]: Held 0.9", "labels": [{"name": "bot-wrong"}]},
          {"number": 10, "state": "OPEN", "title": "[App Update]: Tagged 6.2_x86_64"}]
app_updates.gh = lambda *args, **kw: calls.append(args) or (json.dumps(issues) if args[:2] == ("issue", "list") else "")
apps = {p: {"package": p, "name": n, "current": c, "current_version_codes": []} for p, n, c in [
    ("cx", "Cx File Explorer", "2.7.8"), ("showly", "Showly", "3.70.0"), ("audible", "Audible", "26.38.08"),
    ("new", "Newcomer", "1.0"), ("moviebox", "MovieBox", "4.0.03.0918.03"), ("tv", "MovieBox TV", "1.2.0.0923"),
    ("held", "Held", "1.0"), ("tagged", "Tagged", "6.0_arm64")]}
record = {"status": "update", "reported_by": ["play"], "confirmed_by": ["play"], "readiness": "ready"}
app_updates.sync_issues(apps, [{**record, "package": "cx", "candidate": "2.7.9"},
                               {**record, "package": "showly", "candidate": "3.72.0"},
                               {**record, "package": "audible", "candidate": None, "status": "current"},
                               {**record, "package": "new", "candidate": "1.1"},
                               {**record, "package": "moviebox", "candidate": None, "status": "current"},
                               {**record, "package": "held", "candidate": "2.0"},
                               {**record, "package": "tagged", "candidate": "6.1_arm64"}], "1.38.0")
actions = [c[1:3] for c in calls if c[0] == "issue" and c[1] != "list"]
assert actions == [("edit", "1"), ("edit", "2"), ("close", "4"), ("edit", "7"), ("create", "--title"), ("create", "--title")], actions
assert ("issue", "edit", "2", "--title", "[App Update]: Showly 3.72.0") == next(c for c in calls if c[1:3] == ("edit", "2"))
assert ("issue", "edit", "7", "--title", "[App Update]: Newcomer 1.1") == next(c for c in calls if c[1:3] == ("edit", "7"))
close = next(c for c in calls if c[1] == "close")
assert close[:4] == ("issue", "close", "4", "--comment") and close[4].startswith("Audible `26.38.08` support has been added in Bundle [v1.38.0](https://github.com/hxreborn/morphe-patches/releases/tag/v1.38.0). "), calls
print("issue sync passes")


psiphon = {"current": "2.5.11", "current_version_codes": [506], "any_version": False}
older = app_updates.classify(psiphon, {"aptoide": {"version": "486", "version_code": 486}}, {})
assert older["status"] == "current" and older["candidate"] is None, older
assert older["notes"] == ["behind current: aptoide 486"], older
print("version code comparison passes")


def base_apk_with_dex(folder, version):
    apk = Path(folder) / f"{version}.apk"
    with zipfile.ZipFile(apk, "w") as archive:
        archive.writestr("classes.dex", b"dex\n" + version.encode() + b"\0" + bytes(104))
    return apk


with tempfile.TemporaryDirectory() as folder:
    assert app_updates.play_min_sdk(32, base_apk_with_dex(folder, "038")) == 26
    assert app_updates.play_min_sdk(29, base_apk_with_dex(folder, "039")) == 28
    assert app_updates.play_min_sdk(26, base_apk_with_dex(folder, "039")) == 26
    assert app_updates.play_min_sdk(32, base_apk_with_dex(folder, "035")) is None
    assert app_updates.play_min_sdk(21, base_apk_with_dex(folder, "035")) == 21
    assert app_updates.play_min_sdk(None, base_apk_with_dex(folder, "039")) is None
print("play min SDK passes")
