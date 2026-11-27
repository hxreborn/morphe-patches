import json
import subprocess
import sys
import tempfile
from pathlib import Path

import app_updates
import bump_prs
from versions import variant_targets

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "actions" / "triage"))
from app_update import decide

PACKAGE = "com.reworewo.prayertimes"
NAME = "Prayer Times and Qibla"
MOBILE = f"{NAME} (Mobile)"
WEAR = f"{NAME} (Wear OS)"


def compatibility(factor, version, code, min_sdk):
    return {"packageName": PACKAGE, "name": NAME, "apkFileType": "APKS", "signatures": ["abc"],
            "formFactor": factor, "targets": [{"version": version,
                "versionCodes": {"ARM64_V8A": code, "ARMEABI_V7A": code, "X86_64": code, "X86": code},
                "isExperimental": False, "minSdk": min_sdk, "description": None}]}


metadata = {"version": "1.0", "patches": [{"name": "Patch", "compatiblePackages": [
    compatibility("WEARABLE", "1.3.2", 717, 26), compatibility("MOBILE", "3.8.3", 742, 24)]}]}
assert variant_targets(metadata) == {WEAR: {"1.3.2"}, MOBILE: {"3.8.3"}}
real_apps = app_updates.load_inventory("patches-list.json")
real_metadata = json.loads(Path("patches-list.json").read_text())
real_targets = variant_targets(real_metadata)
for factor, label in (("MOBILE", MOBILE), ("WEARABLE", WEAR)):
    assert {target["version"] for target in real_apps[PACKAGE, factor]["targets"]} == real_targets[label]


def body(app, version):
    return f"### App\n\n{app}\n\n### New version\n\n{version}\n\n### APK source\n\nhttps://example.test\n"


def issue(number, app, version):
    return {"number": number, "state": "OPEN", "title": f"update({app}): Support {version}"}


result = decide(1, body(WEAR, "1.3.3"), [issue(2, MOBILE, "3.8.4")], metadata)
assert result["verdict"] == "pass" and result["title"] == f"update({WEAR}): Support 1.3.3", result
assert decide(1, body(NAME, "1.3.3"), [], metadata)["verdict"] == "flag"
assert decide(1, body(WEAR, "1.3.3"), [issue(2, WEAR, "1.3.3")], metadata)["duplicate_of"] == 2
assert decide(1, body(WEAR, "1.3.2"), [], metadata)["reason"] == "completed"

with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    path = root / "patches-list.json"
    path.write_text(json.dumps(metadata))
    apps = app_updates.load_inventory(path)
    mobile = apps[PACKAGE, "MOBILE"]
    wear = apps[PACKAGE, "WEARABLE"]
    assert (mobile["current"], wear["current"]) == ("3.8.3", "1.3.2")
    assert mobile["current_version_codes"] == [742] and wear["current_version_codes"] == [717]
    assert not app_updates.should_check_sources(mobile) and not app_updates.should_check_sources(wear)

    unnamed = {"patches": [{"name": "Patch", "compatiblePackages": [{"packageName": PACKAGE}]}]}
    unnamed_path = root / "unnamed.json"
    unnamed_path.write_text(json.dumps(unnamed))
    assert app_updates.load_inventory(unnamed_path)[PACKAGE, "MOBILE"]["name"] == PACKAGE

    templates = root / ".github" / "ISSUE_TEMPLATE"
    templates.mkdir(parents=True)
    for name in ("app_update.yml", "bug_report.yml", "not_working.yml"):
        (templates / name).write_text("        # generated:apps\n        # /generated\n")
    subprocess.run([sys.executable, str(Path(__file__).with_name("generate_issue_forms.py").resolve()), str(path)],
                   cwd=root, check=True, capture_output=True)
    generated = (templates / "app_update.yml").read_text()
    assert json.dumps(MOBILE) in generated and json.dumps(WEAR) in generated

    calls = []
    issues = [issue(2, WEAR, "1.3.3"), issue(3, NAME, "1.3.3"), issue(4, MOBILE, "3.8.3")]
    app_updates.gh = lambda *args, **kwargs: calls.append(args) or (json.dumps(issues) if args[:2] == ("issue", "list") else "")
    records = [{**app, "status": "unchecked", "candidate": None} for app in (mobile, wear)]
    app_updates.sync_issues(apps, records, "1.0")
    closed = [call[2] for call in calls if call[:2] == ("issue", "close")]
    assert closed == ["4"], calls

source = '''    val MOBILE = Compatibility(
        name = "Prayer Times and Qibla",
        packageName = "com.reworewo.prayertimes",
        targets = listOf(AppTarget(version = "3.8.3", versionCode = 742, minSdk = 24)),
    )
    val WEAR = Compatibility(
        name = "Prayer Times and Qibla",
        packageName = "com.reworewo.prayertimes",
        targets = listOf(AppTarget(version = "1.3.2", versionCode = 717, minSdk = 26)),
        formFactor = FormFactor.WEARABLE,
    )
'''
updated = bump_prs.add_target(source, PACKAGE, "1.3.3", 718, 26, "WEARABLE")
assert 'AppTarget(version = "1.3.3", versionCode = 718, minSdk = 26)' in updated
assert updated.split("    val WEAR")[0] == source.split("    val WEAR")[0]
factor_first = source.replace('        packageName = "com.reworewo.prayertimes",\n        targets = listOf(AppTarget(version = "1.3.2", versionCode = 717, minSdk = 26)),\n        formFactor = FormFactor.WEARABLE,',
                              '        formFactor = FormFactor.WEARABLE,\n        packageName = "com.reworewo.prayertimes",\n        targets = listOf(AppTarget(version = "1.3.2", versionCode = 717, minSdk = 26)),')
assert 'version = "1.3.3"' in bump_prs.add_target(factor_first, PACKAGE, "1.3.3", 718, 26, "WEARABLE")
try:
    bump_prs.add_target(source, PACKAGE, "1.3.3", 718, 26)
    raise AssertionError("ambiguous package accepted")
except ValueError as error:
    assert "2 declarations" in str(error)
print("form factor inventory, triage, issue sync, picker and bump checks pass")
