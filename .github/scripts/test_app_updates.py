import json
import sys
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
          {"number": 10, "state": "OPEN", "title": "[App Update]: Tagged 6.2_x86_64"},
          {"number": 11, "state": "OPEN", "title": "[App Update]: Audible 26.38.08",
           "body": "### Patching this version\n\nPatching works, the app misbehaves\n"},
          {"number": 12, "state": "OPEN", "title": "[App Update]: Audible 26.38.08",
           "body": "### Patching this version\n\nTried but patching failed\n"}]
app_updates.gh = lambda *args, **kw: calls.append(args) or (json.dumps(issues) if args[:2] == ("issue", "list") else "")
apps = {(p, "MOBILE"): {"package": p, "name": n, "current": c, "current_version_codes": []} for p, n, c in [
    ("cx", "Cx File Explorer", "2.7.8"), ("showly", "Showly", "3.70.0"), ("audible", "Audible", "26.38.08"),
    ("new", "Newcomer", "1.0"), ("moviebox", "MovieBox", "4.0.03.0918.03"), ("tv", "MovieBox TV", "1.2.0.0923"),
    ("held", "Held", "1.0"), ("tagged", "Tagged", "6.0_arm64")]}
record = {"form_factor": "MOBILE", "status": "update", "reported_by": ["play"], "confirmed_by": ["play"], "readiness": "ready"}
app_updates.sync_issues(apps, [{**record, "package": "cx", "candidate": "2.7.9"},
                               {**record, "package": "showly", "candidate": "3.72.0"},
                               {**record, "package": "audible", "candidate": None, "status": "current"},
                               {**record, "package": "new", "candidate": "1.1"},
                               {**record, "package": "moviebox", "candidate": None, "status": "current"},
                               {**record, "package": "held", "candidate": "2.0"},
                               {**record, "package": "tagged", "candidate": "6.1_arm64"}], "1.38.0")
label = json.loads(Path(".github/labels.json").read_text())["labels"]["app-update"]
assert calls[0] == ("label", "create", "app-update", "--force", "--color", label["color"],
                    "--description", label["description"]), calls[0]
actions = [c[1:3] for c in calls if c[0] == "issue" and c[1] != "list"]
assert actions == [("edit", "1"), ("edit", "2"), ("close", "4"), ("edit", "7"), ("create", "--title"), ("create", "--title")], actions
assert ("issue", "edit", "2", "--title", "[App Update]: Showly 3.72.0") == next(c for c in calls if c[1:3] == ("edit", "2"))
assert ("issue", "edit", "7", "--title", "[App Update]: Newcomer 1.1") == next(c for c in calls if c[1:3] == ("edit", "7"))
close = next(c for c in calls if c[1] == "close")
assert close[:4] == ("issue", "close", "4", "--comment") and close[4].startswith("Audible `26.38.08` support has been added in Bundle [v1.38.0](https://github.com/hxreborn/morphe-patches/releases/tag/v1.38.0). "), calls
calls.clear()
app_updates.sync_issues(apps, [{**record, "package": "new", "candidate": "1.2", "readiness": "untested"},
                               {**record, "package": "tagged", "candidate": "6.3_arm64", "readiness": "untested"}], "1.38.0")
assert [c[1:3] for c in calls if c[0] == "issue" and c[1] != "list"] == [], calls
sys.path.insert(0, str(Path(".github/actions/triage").resolve()))
from app_update import form_options
from test_real_reports import DUOLINGO_UPDATE_1048

RESULT_CHOICES = {"Not tried": True, "Patching fails": False, "Patching works, the app misbehaves": False,
                  "Patching works and so does the app": True}
assert set(form_options("Patching this version", Path(".github/ISSUE_TEMPLATE/app_update.yml"))) == set(RESULT_CHOICES)
calls.clear()
choice_issues = [{"number": 100 + n, "state": "OPEN", "title": "[App Update]: Duolingo 6.106.6",
                  "body": DUOLINGO_UPDATE_1048.replace("Patching fails", choice)}
                 for n, choice in enumerate(RESULT_CHOICES)]
app_updates.gh = lambda *args, **kw: calls.append(args) or (json.dumps(choice_issues) if args[:2] == ("issue", "list") else "")
app_updates.sync_issues({("com.duolingo", "MOBILE"): {"package": "com.duolingo", "name": "Duolingo", "current": "6.106.6",
                                                      "current_version_codes": []}},
                        [{**record, "package": "com.duolingo", "candidate": None, "status": "current"}], "1.89.0")
closed_numbers = {c[2] for c in calls if c[:2] == ("issue", "close")}
assert closed_numbers == {str(100 + n) for n, closes in enumerate(RESULT_CHOICES.values()) if closes}, closed_numbers
print("Scheduled sync keeps a supported-version failure open for every failure choice")
BEREAL_LOG = """INFO: Filtering patches for com.bereal.ft v3.99.1...
INFO: Skipping disabled: Override certificate pinning (default)
INFO: Skipping disabled: Remove Friends of Friends tab (default)
INFO: Applying 20 patches...
INFO: Deleting existing temporary files directory
INFO: Decoding the manifest
INFO: Decoding resources on request, 4010 files can be decoded
INFO: Executing patches
INFO: Building instruction indexes for 99220 classes...
INFO: Applied: Back up memories
INFO: Applied: Change download location
INFO: Applied: Custom location
INFO: Applied: Custom retake count
INFO: Applied: Disable data collection
INFO: Applied: Disable typing indicator
INFO: Applied: Disable view receipts
INFO: Applied: Post from gallery
INFO: Applied: Remove Friends of Friends prompts
INFO: Applied: Remove ads
INFO: Applied: Remove contacts permission
INFO: Applied: Remove friend suggestions
INFO: Applied: Remove late flag
INFO: Applied: Remove late label
INFO: Applied: Remove promotions
INFO: Applied: Remove streak prompts
INFO: Applied: Remove update prompts
INFO: Applied: Remove watermark
ERROR: Failed: Save friends' BeReals
INFO: Patching took 3.0 seconds
ERROR: Patching aborted: Patching failed: Patch "Save friends' BeReals" failed: Failed to match the fingerprint: app.morphe.patches.bereal.misc.download.FeedPostMenuFingerprint@17df049f
ERROR: app.morphe.patcher.patch.PatchException: Failed to match the fingerprint: app.morphe.patches.bereal.misc.download.FeedPostMenuFingerprint@17df049f
	at app.morphe.patcher.Fingerprint.patchException(Fingerprint.kt:2265)
	at app.morphe.patcher.Fingerprint.checkMatchesRange(Fingerprint.kt:2402)
	at app.morphe.patcher.Fingerprint.matchAll(Fingerprint.kt:2391)
	at app.morphe.patcher.util.BytecodeUtilsKt.matchSingle(BytecodeUtils.kt:91)
	at app.morphe.patcher.util.BytecodeUtilsKt.matchSingle(BytecodeUtils.kt:831)
	at app.morphe.patches.bereal.misc.download.SaveFriendsBeRealsPatchKt.saveFriendsBeRealsPatch$lambda$0$0(SaveFriendsBeRealsPatch.kt:47)
	at app.morphe.patcher.patch.Patch.execute(Patch.kt:160)
	at app.morphe.patcher.patch.BytecodePatch.execute$morphe_patcher(Patch.kt:357)

INFO: Use --continue-on-error to skip failed patches and continue patching
"""
BEREAL_PATCHES = ["Back up memories", "Change download location", "Custom location", "Custom retake count", "Disable data collection",
                  "Disable typing indicator", "Disable view receipts", "Post from gallery", "Remove Friends of Friends prompts",
                  "Remove Friends of Friends tab", "Remove ads", "Remove contacts permission", "Remove friend suggestions",
                  "Remove late flag", "Remove late label", "Remove promotions", "Remove streak prompts", "Remove update prompts",
                  "Remove watermark", "Save friends' BeReals", "Show friends' posts before posting"]
FINGERPRINT_FAILURE = "Failed to match the fingerprint: app.morphe.patches.bereal.misc.download.FeedPostMenuFingerprint"
bereal_check = app_updates.parse_patch_log(BEREAL_LOG, 1)
assert bereal_check["failed"] == ["Save friends' BeReals"], bereal_check
assert bereal_check["skipped"] == ["Override certificate pinning", "Remove Friends of Friends tab"], bereal_check
assert len(bereal_check["applied"]) == 18 and "Show friends' posts before posting" not in bereal_check["applied"], bereal_check
assert bereal_check["reasons"] == {"Save friends' BeReals": FINGERPRINT_FAILURE}, bereal_check
bereal_app = {"name": "BeReal", "package": "com.bereal.ft", "current": "3.98.0", "current_version_codes": [3600852], "patches": BEREAL_PATCHES,
              "sources": {"play": {"version": "3.99.1"}, "apkmirror": {"error": "not listed"}, "uptodown": {"version": "3.100.0"},
                          "apkpure": {"version": "3.99.1"}}}
bereal_record = {"candidate": "3.99.1", "verification": {"3.99.1": [{"source": "play", "package_ok": True, "version_ok": True, "cert_ok": True,
                 "version_code": 3603560, "patch_check": bereal_check}]}}
body = app_updates.issue_body(bereal_app, bereal_record)
assert "BeReal `3.98.0` → `3.99.1`" in body and "Package `com.bereal.ft`, version code 3600852 → 3603560" in body, body
assert "- [x] Version confirmed by Play and APKPure\n" in body and "- [x] Back up memories applies" in body, body
assert body.count("fails to apply") == 1 and "- [ ] **Save friends' BeReals fails to apply**" in body, body
assert "- [ ] Show friends' posts before posting not reached\n" in body and "**Show friends'" not in body, body
assert "Remove Friends of Friends tab" not in body and "Override certificate pinning" not in body, body
assert f"| Save friends' BeReals | `{FINGERPRINT_FAILURE}` |" in body and "| Show friends'" not in body, body
assert "@" not in next(line for line in body.splitlines() if line.startswith("| Save friends'")), body
assert "Device check" not in body and "<details open><summary>Patcher log</summary>" in body, body
assert "| Play | 3.99.1 | APK downloaded |" in body and "| APKPure | 3.99.1 |  |" in body, body
assert "| Uptodown | 3.100.0 | newer, not verified |" in body and "APKMirror" not in body, body
assert app_updates.readiness({"patches": BEREAL_PATCHES}, {"status": "update", "candidate": "3.99.1", "verification": bereal_record["verification"]}) == "needs work: 1 failed"
clean_log = "\n".join(line for line in BEREAL_LOG.splitlines() if "Failed" not in line and "failed" not in line and not line.startswith(("\t", "ERROR", "INFO: Use")))
clean_check = app_updates.parse_patch_log(clean_log + "\nINFO: Applied: Save friends' BeReals\nINFO: Applied: Show friends' posts before posting\n", 0)
clean_record = {"candidate": "3.99.1", "verification": {"3.99.1": [{**bereal_record["verification"]["3.99.1"][0], "patch_check": clean_check}]}}
clean_body = app_updates.issue_body(bereal_app, clean_record)
assert "- [ ] Device check" in clean_body and "not reached" not in clean_body and "### Failure" not in clean_body, clean_body
assert "Remove Friends of Friends tab" not in clean_body, clean_body
newer_bundle_body = app_updates.issue_body({**bereal_app, "patches": [*BEREAL_PATCHES, "Added after the checked bundle"]}, clean_record)
assert "- [ ] Added after the checked bundle not in the checked bundle\n" in newer_bundle_body and "Device check" not in newer_bundle_body, newer_bundle_body
assert app_updates.readiness({"patches": BEREAL_PATCHES}, {"status": "update", "candidate": "3.99.1", "verification": clean_record["verification"]}) == "ready"
crash_check = app_updates.parse_patch_log("INFO: Applied: Back up memories\nSEVERE: Exception in thread main\n", 1)
crash_body = app_updates.issue_body(bereal_app, {"candidate": "3.99.1", "verification": {"3.99.1": [{**bereal_record["verification"]["3.99.1"][0], "patch_check": crash_check}]}})
assert "### Failure\n\n<details open>" in crash_body and "| Patch | Error |" not in crash_body and "fails to apply" not in crash_body, crash_body
sent = []
app_updates.gh = lambda *args, **kw: sent.append(kw.get("input")) or ("[]" if args[:2] == ("issue", "list") else "")
app_updates.sync_issues({("com.bereal.ft", "MOBILE"): {k: v for k, v in bereal_app.items() if k != "sources"}},
                        [{**bereal_app, **bereal_record, "form_factor": "MOBILE", "status": "update", "reported_by": ["play", "apkpure"], "readiness": "needs work: 1 failed"}], "1.38.0")
assert [text for text in sent if text] == [body], sent
print("issue body passes")
print("issue sync passes")


psiphon = {"current": "2.5.11", "current_version_codes": [506], "any_version": False}
older = app_updates.classify(psiphon, {"aptoide": {"version": "486", "version_code": 486}}, {})
assert older["status"] == "current" and older["candidate"] is None, older
assert older["notes"] == ["behind current: aptoide 486"], older
print("version code comparison passes")


class Reply:
    def __init__(self, payload, status=200):
        self.payload, self.status_code = payload, status

    def json(self):
        return self.payload

    def raise_for_status(self):
        assert self.status_code < 400, self.status_code


FDROID_PASS = {"packageName": "proton.android.pass.fdroid", "suggestedVersionCode": 14105000,
               "packages": [{"versionName": "1.41.5", "versionCode": 14105000},
                            {"versionName": "1.41.4", "versionCode": 14104000}]}
VPN_RELEASE = {"tag_name": "5.21.57.0", "html_url": "https://github.com/ProtonVPN/android-app/releases/tag/5.21.57.0",
               "assets": [{"name": "ProtonVPN-5.21.57.0.605215700.-production-vanilla-direct-release.apk",
                           "browser_download_url": "https://example.test/direct.apk"},
                          {"name": "ProtonVPN-5.21.57.0.605215700.-production-vanilla-openSource-release-fdroid.apk",
                           "browser_download_url": "https://example.test/fdroid.apk"}]}
MAIL_RELEASE = {"tag_name": "7.12.1(19175)", "html_url": "https://example.test/mail",
                "assets": [{"name": "ProtonMail-7.12.1_19175.apk", "browser_download_url": "https://example.test/mail.apk"}]}
RESPONSES = {
    app_updates.FDROID_API + "proton.android.pass.fdroid": Reply(FDROID_PASS),
    app_updates.FDROID_API + "ch.protonmail.android": Reply({"error": "NOT_FOUND"}, 404),
    app_updates.GITHUB_API + "ProtonVPN/android-app/releases/latest": Reply(VPN_RELEASE),
    app_updates.GITHUB_API + "ProtonMail/android-mail/releases/latest": Reply(MAIL_RELEASE),
}
real_get = app_updates.requests.get
app_updates.requests.get = lambda url, **_: RESPONSES[url]
try:
    assert app_updates.lookup_fdroid({"package": "proton.android.pass.fdroid"}, {}) == {
        "version": "1.41.5", "version_code": 14105000,
        "download": "https://f-droid.org/repo/proton.android.pass.fdroid_14105000.apk",
        "url": "https://f-droid.org/packages/proton.android.pass.fdroid/"}
    assert app_updates.lookup_fdroid({"package": "ch.protonmail.android"}, {}) == {"error": "not listed"}
    direct = app_updates.lookup_github({"package": "ch.protonvpn.android"}, {})
    assert (direct["version"], direct["version_code"], direct["download"]) == ("5.21.57.0", 605215700, "https://example.test/direct.apk")
    fdroid = app_updates.lookup_github({"package": "ch.protonvpn.android.reproducible"}, {})
    assert fdroid["download"] == "https://example.test/fdroid.apk", fdroid
    mail = app_updates.lookup_github({"package": "ch.protonmail.android"}, {})
    assert (mail["version"], mail["version_code"]) == ("7.12.1", 19175), mail
    assert app_updates.lookup_github({"package": "com.example.other"}, {}) == {"error": "not listed"}
finally:
    app_updates.requests.get = real_get
print("F-Droid and GitHub release lookups pass")


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
