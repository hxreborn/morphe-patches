#!/usr/bin/env python3

import copy
import json
import subprocess
import sys
import tempfile
from pathlib import Path

from versions import APK_DOWNLOAD_WARNING, APK_VERSION_URLS

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / ".github/actions/triage"))
from app_update import decide
from replies import supported_downloads

PACKAGE = "com.community.mbox.tv"
VERSION = "1.2.0.0923.web_socialmedia"
URL = APK_VERSION_URLS[(PACKAGE, VERSION)]
PATCHES = {
    "version": "1.75.0",
    "patches": [{
        "name": "All-In-One",
        "compatiblePackages": [{
            "name": "MovieBox TV",
            "packageName": PACKAGE,
            "targets": [{"version": VERSION}],
        }],
    }],
}

downloads = supported_downloads(PATCHES, "MovieBox TV")
assert f"[MovieBox TV {VERSION}]({URL})" in downloads, downloads
assert APK_DOWNLOAD_WARNING in downloads, downloads
assert not supported_downloads(PATCHES, "MovieBox")
assert supported_downloads({**PATCHES, "patches": PATCHES["patches"] * 2}, "MovieBox TV") == downloads

retargeted = copy.deepcopy(PATCHES)
retargeted["patches"][0]["compatiblePackages"][0]["targets"] = [{"version": "1.2.2.1106.03"}]
assert not supported_downloads(retargeted, "MovieBox TV")

body = "### App\n\nMovieBox TV\n\n### New version\n\n1.2.2.1106.03 (50040021)\n"
result = decide(682, body, [], PATCHES)
assert result["verdict"] == "pass", result
assert result["comment"] == "<!-- app-update-bot -->\n" + downloads, result
assert not decide(682, body, [], retargeted)["comment"].endswith(downloads)

with tempfile.TemporaryDirectory() as temp:
    directory = Path(temp)
    patches = directory / "patches.json"
    patches.write_text(json.dumps(PATCHES))
    report = directory / "report.txt"
    report.write_text(
        "### App and version\n\nMovieBox TV 1.2.2.1106.03\n\n"
        "### Bug description\n\nPatching stops with an error before it writes an APK file.\n"
    )
    triage = subprocess.run(
        [sys.executable, str(ROOT / ".github/actions/triage/triage.py"), str(report), str(patches)],
        capture_output=True, text=True, check=True,
    )
    result = json.loads(triage.stdout)
    assert "untargeted version" in result["labels"], result
    assert downloads in result["comment"], result

    readme = directory / "README.md"
    readme.write_text("<!-- PATCHES_START -->\n<!-- PATCHES_END -->\n")
    readme_patches = json.loads((ROOT / "patches-list.json").read_text())
    readme_patches["patches"].append(PATCHES["patches"][0])
    patches.write_text(json.dumps(readme_patches))
    subprocess.run(
        [sys.executable, str(ROOT / ".github/scripts/generate_patches_readme.py"),
         "hxreborn/morphe-patches", "dev", str(patches), str(readme)],
        cwd=ROOT, capture_output=True, text=True, check=True,
    )
    assert f'[{VERSION}]({URL} "Download stock APK")' in readme.read_text()
    assert APK_DOWNLOAD_WARNING in readme.read_text()

print("PASS supported APK links, target removal, app-update reply, triage reply and README version link")
