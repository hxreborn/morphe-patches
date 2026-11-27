#!/usr/bin/env python3
"""python3 .github/actions/triage/test_real_reports.py"""

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[1] / "scripts"))
from app_update import decide as update_verdict
from app_update import failure_choices, form_options
from feature_request import decide as feature_verdict

TRIAGE = HERE / "triage.py"
FORM = HERE.parents[1] / "ISSUE_TEMPLATE" / "app_update.yml"
REPORTED_BUNDLES = ("1.80.0", "1.81.0", "1.84.0", "1.84.0-dev.1", "1.87.0", "2.4.0")


def package(name, package_name, *versions):
    return {"name": name, "packageName": package_name, "targets": [{"version": v} for v in versions]}


PATCHES = {"version": "1.89.0-dev.5", "patches": [{"compatiblePackages": [
    package("MovieBox TV", "com.community.mbox.tv", "1.2.3.1113.03"),
    package("MovieBox", "com.community.oneroom", "4.0.03.1020.03"),
    package("Ground News", "com.checkitt", "5.1.0"),
    package("VPN Super Unlimited Proxy", "com.free.vpn.super.hotspot.open", "2.36.0"),
    package("imo", "com.imo.android.imoim", "2026.10.1031"),
    package("Duolingo", "com.duolingo", "6.106.6"),
    package("Proton VPN", "ch.protonvpn.android", "5.21.57.0"),
    package("YouTube Music", "com.google.android.apps.youtube.music", "9.30.53"),
    package("Audiomack", "com.audiomack", "8.11.4"),
]}]}


MOVIEBOX_15_MINUTES_1074 = '''### App and version

MovieBox 4.0.03.1020.03

### Other version

_No response_

### Patches version

1.87.0

### What does not work

Something else the app used to do stopped working

### Did this work before

Yes, with an older patches release on the same app version

### Does the unpatched app do the same thing

I have not tried the unpatched app

### Patch that breaks it

None

### Device and Android version

Oneplus13r, Android 17

### APK source

https://com-community-oneroom.en.uptodown.com/android/download/1229268575-x

### Architecture

arm64-v8a

### Patched with

Morphe Manager

### "Optimize for device architecture" in Manager's Advanced settings

Off

### Other bundles enabled

_No response_

### Steps to reproduce

lots of content are limited to a 15min duration only. before it used to have full duration of the movies

### Expected behaviour

should show the entire duration. for eg. 1hr movie shows 15:02mins only, and similarly for a 3hr movie as well.

### Actual behaviour

above issue

### Screenshots

above issue

### Error logs

_No response_

### Acknowledgements

- [x] I repatched with the newest release of this bundle and it still happens.
- [x] I uninstalled the app before installing the patched build.
- [x] Nothing personal in my screenshots or logs.'''


MOVIEBOX_WHITE_SCREEN_933 = '''### App and version

MovieBox 4.0.03.1020.03

### Other version

_No response_

### Patches version

1.81.0

### What does not work

The app will not open at all

### Did this work before

I have not checked

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Patch that breaks it

None

### Device and Android version

Samsung A36 , Android 17

### APK source

https://com-community-oneroom.en.uptodown.com/android/download/1229268575-x

### Architecture

arm64-v8a

### Patched with

Morphe Manager

### "Optimize for device architecture" in Manager's Advanced settings

On

### Other bundles enabled

_No response_

### Steps to reproduce

Just Patched and then the app not opening , tap opens , 1 milli sec white screen , back to home again same thing and on repeat

### Expected behaviour

1 milli Sec white screen then crash

### Actual behaviour

And it actually does this every single time

### Screenshots

app doesn't even open so how to screen shot it in 1 milli sec 

### Error logs


Logs : 

[INFO] Build: manager=2.6.0 patcher=2.4.0 nativeLibs=true
[INFO] Patching started at 1794951622197 pkg=com.community.oneroom version=4.0.03.1020.03 input=/data/user/0/app.morphe.manager/app_ui_ephemeral/temp_apk_1794951618107.apk size=68487317 split=false arch=ARM64_V8A patches=4
[INFO] Source: name="hxreborn’s patches" version="1.81.0"
[INFO] Runtime: name=jvm memoryLimit=2450
[INFO] Patcher process: pid=21903 vm=JVM heap=2450MB bits=64 abis=arm64-v8a age=8s
[INFO] Patcher JVM: libjvm=server/libjvm.so options=-XX:+UseCompactObjectHeaders -XX:AOTCache=/data/user/0/app.morphe.manager/cache/jvm-aot/runtime-arm64-v8a-8c33713d-host-39922687-0.aot
[INFO] Host VM: OpenJDK 64-Bit Server VM 25.0.5-internal-adhoc.runner.openjdk (mixed mode, sharing) arch=aarch64 cores=8
[INFO] Heap limit: 2450MB
[INFO] Loaded 572 patches of patches.jar ahead in 3.049s
[INFO] Applying patches...
[INFO] Deleting existing temporary files directory
[INFO] Decoding the manifest
[INFO] Decoding resources on request, 6626 files can be decoded
[INFO] Executing patches
[WARN] Unsupported Ijiami native packer build for armeabi-v7a: LYouKWv/armeabi/libbqhvot.so sha-256 87853817abc711c6397426fd6c0ebe002856493d62c9e304969f20b4be205fae
[INFO] Native packer profile for arm64-v8a: 2d62d8b7
[INFO] Component factory: uvj.ucxx.mb.QiQ9O3z -> hx.Delegate (classes2.dex)
[INFO] Disabled Lcom/ijiami/residconfusion/ConfusionUtils;->check
[INFO] All-In-One succeeded
[INFO] Hide BuzzBox succeeded
[INFO] Hide Midnight tab succeeded
[INFO] Hide permanent notification succeeded
[INFO] Writing patched files...
[INFO] Compiling patched dex files (mode: NONE)
[INFO] Compiling modified resources
[INFO] Stripping libs (keeping architectures arm64-v8a)
[INFO] Stripped 29 lib files
[INFO] Decoded 0 of 6626 files on request
[INFO] Unescaping strings
[INFO] Processing aapt macros
[INFO] Generating new resource IDs
[INFO] Writing resource APK
[INFO] Reusing 6266 unchanged archive entries, rebuilding 2 entries, dropping 30 entries, leaving out 3 replaced entries
[INFO] Resource APK scan: 100ms write: 1.8s
[INFO] Patched apk saved to /data/user/0/app.morphe.manager/app_ephemeral/patched.apk (patch=10.1s compile=2.36s write=0s)
[INFO] Heap after patching: average=399MB max=922MB
[INFO] Usage after patching: ioPeak=47292
[INFO] Signed apk in 286ms
[INFO] Patching succeeded: output=/data/user/0/app.morphe.manager/app_ui_ephemeral/installer/output.apk size=61427968 elapsed=12948ms

### Acknowledgements

- [x] I repatched with the newest release of this bundle and it still happens.
- [x] I uninstalled the app before installing the patched build.
- [x] Nothing personal in my screenshots or logs.'''


VPN_SUPER_SHORT_983 = '''### App and version

VPN Super Unlimited Proxy 2.36.0

### Other version

_No response_

### Patches version

1.84.0

### What does not work

Something else the app used to do stopped working

### Did this work before

I have not checked

### Does the unpatched app do the same thing

I have not tried the unpatched app

### Patch that breaks it

None

### Device and Android version

Nothing Phone 3a, android 17

### APK source

https://www.apkmirror.com/apk/vpn-super-inc-2/vpn-super-unlimited-proxy/vpn-super-unlimited-proxy-2-36-0-release/vpn-super-unlimited-proxy-2-36-0-android-apk-download/

### Architecture

arm64-v8a

### Patched with

Morphe Manager

### "Optimize for device architecture" in Manager's Advanced settings

I do not know

### Other bundles enabled

_No response_

### Steps to reproduce

Open patched app

### Expected behaviour

working

### Actual behaviour

not working

### Screenshots

<img width="1080" height="2392" alt="image" src="https://github.com/user-attachments/assets/72b48f4a-4ba2-4f96-8d3b-2329b4a6ad1a" />

### Error logs

_No response_

### Acknowledgements

- [x] I repatched with the newest release of this bundle and it still happens.
- [x] I uninstalled the app before installing the patched build.
- [x] Nothing personal in my screenshots or logs.'''


IMO_PATCHING_FAILED_960 = '''### What happened

Patching started and stopped with an error

### Did this work before

Yes, with an older patches release on the same app version

### App and version

imo 2026.10.1031

### Other version

_No response_

### Patched with

Morphe Manager

### APK source

Apkmirror

### Architecture

arm64-v8a

### Bug description

Imo patch failed with gsm core support patch 
Both last dev and latest stable patch
Working good with  dev4 patch

### Error logsApp: imo
Package: com.imo.android.imoim
Version: 2026.10.1031
Patches: 32
Failed patch: GmsCore support
Source: hxreborn’s patches 1.84.0-dev.1
Manager: 2.7.0-dev.6
Patcher: 2.5.0-dev.4
Libraries: stripped
Android: 14 (API 34)

app.morphe.patcher.patch.PatchException: The patch "GmsCore support" depends on "Spoof signature", which raised an exception
Caused by: app.morphe.patcher.patch.PatchException: The patch "Spoof signature" depends on "SpoofSignaturePatch.spoofSecurityLibrarySignaturePatch", which raised an exception
Caused by: app.morphe.patcher.patch.PatchException: The app being patched is not signed
	at app.morphe.patches.shared.misc.signature.SpoofSignatureKt.stockSigningCertificate(SpoofSignature.kt:54)
	at app.morphe.patches.imo.misc.signin.SpoofSignaturePatchKt.spoofSecurityLibrarySignaturePatch$lambda$0$0(SpoofSignaturePatch.kt:61)
	at app.morphe.patcher.patch.Patch.execute(Patch.kt:160)
	at app.morphe.patcher.patch.RawResourcePatch.execute$morphe_patcher(Patch.kt:429)
Caused by: app.morphe.util.NoCertificateException: Unable to extract certificate from apk
	at app.morphe.util.CertificateUtilsKt.getEndEntityCertificate(CertificateUtils.kt:31)
	at app.morphe.patches.shared.misc.signature.SpoofSignatureKt.stockSigningCertificate(SpoofSignature.kt:52)
	... 3 more


<img width="1080" height="2400" alt="Screenshot_2026-11-20-03-15-27-976_app morphe manager" src="https://github.com/user-attachments/assets/f7c8abb8-b7b5-4fca-a47c-7920248bdf2c" />
<img width="1080" height="2400" alt="Screenshot_2026-11-20-03-15-23-178_app morphe manager" src="https://github.com/user-attachments/assets/c1a87a2a-4c16-47a8-9af9-371f9e6fd651" />
<img width="1080" height="2400" alt="Screenshot_2026-11-20-03-14-51-094_app morphe manager" src="https://github.com/user-attachments/assets/e5be1020-f4d0-4a17-bb8d-45570e15beb8" />

### Acknowledgements

- [x] Nothing personal in my screenshots or logs.'''


GROUND_NEWS_NOTIFICATIONS_910 = '''### App and version

Ground News 5.1.0

### Other version

_No response_

### Patches version

1.80.0

### What does not work

Something else the app used to do stopped working

### Did this work before

No, it has never worked for me

### Does the unpatched app do the same thing

The unpatched app works fine, only the patched one fails

### Patch that breaks it

Not narrowed down yet

### Device and Android version

Fairphone FP5, Android 15

### APK source

https://www.apkmirror.com/apk/snapwise-inc/ground-news/ground-news-5-1-0-release/ground-news-5-1-0-android-apk-download/

### Architecture

All of them (universal)

### Patched with

Morphe Manager

### "Optimize for device architecture" in Manager's Advanced settings

Off

### Other bundles enabled

_No response_

### Steps to reproduce

N/A

### Expected behaviour

Before patching, I would receive a daily news summary at a time specified within the app as well as regular notifications of breaking stories.

### Actual behaviour

After patching, I haven't received any notifications. In case it's useful, I am not using MicroG as I still use Google Play Services, albeit without a connected Google account.

### Screenshots

<details>
<summary>Screenshots</summary>

<img width="75%" alt="image" src="https://github.com/user-attachments/assets/204b02f7-a231-4372-b64e-c03111e8ec05" />

<img width="75%" alt="image" src="https://github.com/user-attachments/assets/b96da776-68bb-491f-a454-ec1bd1eef267" />

</details>

### Error logs

N/A

### Acknowledgements

- [x] I repatched with the newest release of this bundle and it still happens.
- [x] I uninstalled the app before installing the patched build.
- [x] Nothing personal in my screenshots or logs.'''


AUDIOMACK_SHARING_1069 = '''### App

Audiomack 

### Feature description

please i want to be able to easily share or export my downloaded music to other devices or apps directly from inside the Audiomack app, keeping the real song title and artist name. Right now, downloaded tracks are saved in private app storage (files/Audiomack/) as .bin files named only by track ID with no embedded title/artist metadata, so sharing them locally or playing them in other apps requires manually finding, copying, renaming, and identifying each file.

### Motivation

🆙

### Acknowledgements

- [x] I am not requesting patches for a new app.
- [x] I have checked all open and closed feature requests and this is not a duplicate.
- [x] All requested information has been provided properly.'''


MIRROR_REQUEST_1072 = '''### App

Not applicable

### Feature description

Host a GitLab or Codeberg mirror if GitHub bans or removes this repository.

### Motivation

Many repositories of patches are getting banned or removed. Recently Hoo-dles, and now kveld9 patches. So in case these patches also get banned you can just use GitLab repos or Codeberg instead of fighting with GitHub (which might probably get your account banned, like Paresh)

### Acknowledgements

- [x] I am not requesting patches for a new app.
- [x] I have checked all open and closed feature requests and this is not a duplicate.
- [x] All requested information has been provided properly.'''


UNIVERSAL_PATCHES_934 = '''### App

Universal Patches 

### Feature description

It would be way way amazing if we get universal patches like example for some games which doesn't need internet but if you have internet on they gives ads and you have to use dns.adguard-dns.com in dns settings and same for apps which needs a watch ad thing for example Anilab2 , they show ads for downloads but if you enable dns.adguard-dns.com in settings , boom no ads any way in future ,we can get patches for blocking ads like this and replacing need of dns.adguard-dns.com such things 

### Motivation

Universal Patches for ads ,  what can be better then this ? I mean any offline game or normal app with not heavy ad checking system   excess without ads , sounds so amazing 

### Acknowledgements

- [x] I am not requesting patches for a new app.
- [x] I have checked all open and closed feature requests and this is not a duplicate.
- [x] All requested information has been provided properly.'''


DUOLINGO_UPDATE_1048 = '''### App

Duolingo

### New version

6.106.6

### APK source

https://www.apkmirror.com/apk/duolingo/duolingo-duolingo/duolingo-learn-languages-6-106-6-release/duolingo-learn-languages-6-106-6-2-android-apk-download/

### Patching this version

Patching fails

### Error logs

```shell
Package: com.duolingo
Version: 6.106.6
Patches: 2
Failed patch: Unlock premium
Source: hxreborn’s patches 1.87.0-dev.2
Manager: 2.7.1
Patcher: 2.5.0
Libraries: kept
Android: 17 (API 37)

app.morphe.patcher.patch.PatchException: Failed to match the fingerprint: app.morphe.patches.duolingo.misc.premium.UserIsPaidFingerprint@524d1105
	at app.morphe.patcher.Fingerprint.patchException(Fingerprint.kt:2299)
	at app.morphe.patcher.Fingerprint.match(Fingerprint.kt:2342)
	at app.morphe.patcher.Fingerprint.getInstructionMatches(Fingerprint.kt:2527)
	at app.morphe.patches.duolingo.misc.premium.UnlockPremiumPatchKt.unlockPremiumPatch$lambda$0$0(UnlockPremiumPatch.kt:47)
	at app.morphe.patcher.patch.ContextFreeBlock.invoke(Patch.kt:1006)
	at app.morphe.patcher.patch.ContextFreeBlock.invoke(Patch.kt:1005)
	at app.morphe.patcher.patch.Patch.execute(Patch.kt:160)
	at app.morphe.patcher.patch.Patch.execute$morphe_patcher(Patch.kt:151)
```'''


PROTON_FOSS_UPDATE_1028 = '''### App

Proton VPN

### New version

5.21.57.0

### APK source

https://github.com/ProtonVPN/android-app/releases

### Patching this version

Patching fails

### Error logs

```shell

```'''


UPSTREAM_YOUTUBE_MUSIC_5398 = '''### App

YouTube Music

### App version

9.30.53

### Patches version

2.4.0

### Bug description

When lyrics ends that time still scrolling 

<details>
<summary>Screen recording</summary>

https://github.com/user-attachments/assets/0bf27331-d223-405d-967b-a945fbf4a551

</details>

It will not more scroll(fix) when lyrics are end

<details>
<summary>Screenshot</summary>

<img width="1080" height="2400" alt="Screenshot_20261126-191607 YT Music Morphe" src="https://github.com/user-attachments/assets/7a840a56-6137-416d-950e-8960f4b00e5d" />

</details>

[YT_Music_Morphe_Debug_Logs_2026-11-26.txt](https://github.com/user-attachments/files/35467231/YT_Music_Morphe_Debug_Logs_2026-11-26.txt)

### Error logs

```shell

```

### Acknowledgements

- [x] I have checked all open and closed bug reports and this is not a duplicate.
- [x] All requested information has been provided properly.'''


class RealReports(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        root = Path(cls.directory.name)
        cls.list_path = root / "patches-list.json"
        git = ["git", "-c", "user.name=t", "-c", "user.email=t@t", "-c", "core.hooksPath=/dev/null",
               "-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false", "-C", str(root)]
        subprocess.run([*git, "init", "-q"], check=True)
        for version in REPORTED_BUNDLES:
            cls.list_path.write_text(json.dumps({**PATCHES, "version": version}))
            subprocess.run([*git, "add", "patches-list.json"], check=True)
            subprocess.run([*git, "commit", "-qm", version], check=True)
            subprocess.run([*git, "tag", f"v{version}"], check=True)
        cls.list_path.write_text(json.dumps(PATCHES))

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def triage(self, body, title=""):
        body_path = self.list_path.with_name("body.md")
        body_path.write_text(body)
        result = subprocess.run([sys.executable, str(TRIAGE), str(body_path), str(self.list_path)],
                                env={**os.environ, "ISSUE_TITLE": title, "GITHUB_REPOSITORY": "hxreborn/morphe-patches"},
                                capture_output=True, text=True, check=True)
        return json.loads(result.stdout)

    def test_never_close_a_bug_over_a_missing_screenshot(self):
        for name, body in (("1074", MOVIEBOX_15_MINUTES_1074), ("933", MOVIEBOX_WHITE_SCREEN_933)):
            with self.subTest(issue=name):
                result = self.triage(body)
                self.assertNotEqual("close", result["verdict"])
                self.assertNotIn("Screenshots", result["comment"])

    def test_match_moviebox_rather_than_moviebox_tv_on_a_moviebox_report(self):
        result = self.triage(MOVIEBOX_15_MINUTES_1074)
        self.assertEqual("MovieBox", result["app"])
        self.assertNotIn("untargeted version", result["labels"])

    def test_flag_only_the_untried_stock_app_on_a_report_without_screenshot(self):
        result = self.triage(MOVIEBOX_15_MINUTES_1074)
        self.assertEqual("flag", result["verdict"])
        self.assertEqual(["needs info"], result["labels"])
        self.assertIn("The unpatched app hasn't been tried yet", result["comment"])
        self.assertEqual(1, result["comment"].count("\n- "))

    def test_pass_a_crash_report_whose_screenshot_field_describes_why_there_is_none(self):
        self.assertEqual("pass", self.triage(MOVIEBOX_WHITE_SCREEN_933)["verdict"])

    def test_ask_for_a_screenshot_of_a_visual_problem_without_closing(self):
        body = MOVIEBOX_15_MINUTES_1074.replace(
            "lots of content are limited to a 15min duration only.", "the player layout overlaps the subtitles.").replace(
            "I have not tried the unpatched app", "The unpatched app works fine, only the patched one fails")
        result = self.triage(body)
        self.assertEqual("flag", result["verdict"])
        self.assertEqual([], result["labels"])
        self.assertIn("no screenshot or screen recording", result["comment"])
        shown = body.replace("### Screenshots\n\nabove issue", "### Screenshots\n\n![layout](https://github.com/user-attachments/assets/1)")
        self.assertEqual("pass", self.triage(shown)["verdict"])

    def test_pass_a_report_with_a_placeholder_in_steps_and_logs_when_the_description_is_clear(self):
        self.assertIn("### Steps to reproduce\n\nN/A", GROUND_NEWS_NOTIFICATIONS_910)
        self.assertEqual("pass", self.triage(GROUND_NEWS_NOTIFICATIONS_910)["verdict"])

    def test_flag_a_placeholder_in_steps_when_nothing_describes_the_problem(self):
        body = GROUND_NEWS_NOTIFICATIONS_910.replace(
            "Before patching, I would receive a daily news summary at a time specified within the app as well as regular "
            "notifications of breaking stories.", "Notifications").replace(
            "After patching, I haven't received any notifications. In case it's useful, I am not using MicroG as I still use "
            "Google Play Services, albeit without a connected Google account.", "None")
        result = self.triage(body)
        self.assertEqual("flag", result["verdict"])
        self.assertEqual(["needs info"], result["labels"])

    def test_pass_a_short_but_concrete_report_with_a_screenshot(self):
        result = self.triage(VPN_SUPER_SHORT_983)
        self.assertNotIn("characters", result["comment"])
        self.assertNotIn("description", result["comment"])

    def test_flag_a_description_with_too_few_words(self):
        body = VPN_SUPER_SHORT_983.replace("Open patched app", "crash").replace("working", "x").replace("not x", "y")
        result = self.triage(body.replace("<img", "<span").replace("/>", "></span>"))
        self.assertEqual("flag", result["verdict"])
        self.assertIn("doesn't show what happens", result["comment"])

    def test_accept_a_manager_report_glued_onto_the_error_logs_heading(self):
        self.assertIn("### Error logsApp: imo", IMO_PATCHING_FAILED_960)
        result = self.triage(IMO_PATCHING_FAILED_960)
        self.assertEqual("pass", result["verdict"])
        self.assertEqual({"source": "reported", "version": "1.84.0-dev.1"}, result["manifest"])

    def test_accept_a_manager_report_pasted_under_another_heading(self):
        body = IMO_PATCHING_FAILED_960.replace("### Error logsApp: imo", "### Error logs\n\n_No response_\n\n### Manager report\n\nApp: imo")
        self.assertEqual("pass", self.triage(body)["verdict"])

    def test_accept_an_uploaded_log_file_while_the_error_logs_box_is_empty(self):
        self.assertIn("user-attachments/files/", UPSTREAM_YOUTUBE_MUSIC_5398)
        self.assertEqual("pass", self.triage(UPSTREAM_YOUTUBE_MUSIC_5398)["verdict"])
        patching_failed = "### What happened\n\nPatching started and stopped with an error\n\n" + UPSTREAM_YOUTUBE_MUSIC_5398
        self.assertEqual("pass", self.triage(patching_failed)["verdict"])
        without_file = patching_failed.replace("https://github.com/user-attachments/files/35467231/YT_Music_Morphe_Debug_Logs_2026-11-26.txt",
                                                "log attached to the chat")
        result = self.triage(without_file)
        self.assertEqual("flag", result["verdict"])
        self.assertIn("Patching failed, but the Error logs field has no error report", result["comment"])

    def test_pass_bundle_wide_feature_requests(self):
        for name, body in (("1072", MIRROR_REQUEST_1072), ("934", UNIVERSAL_PATCHES_934)):
            with self.subTest(issue=name):
                result = feature_verdict(body, PATCHES)
                self.assertEqual({"verdict": "pass", "comment": ""}, result)

    def test_still_redirect_a_feature_request_for_a_named_unsupported_app(self):
        result = feature_verdict(MIRROR_REQUEST_1072.replace("Not applicable", "Titans Fam"), PATCHES)
        self.assertEqual("close", result["verdict"])
        self.assertIn("app request", result["comment"])

    def test_pass_a_feature_request_with_a_placeholder_motivation_and_a_clear_description(self):
        self.assertIn("### Motivation\n\n\U0001F199\n", AUDIOMACK_SHARING_1069)
        self.assertEqual({"verdict": "pass", "comment": ""}, feature_verdict(AUDIOMACK_SHARING_1069, PATCHES))

    def test_close_a_feature_request_with_a_placeholder_motivation_and_nothing_described(self):
        body = AUDIOMACK_SHARING_1069.split("### Feature description")[0] + "### Feature description\n\nshare\n\n### Motivation\n\n\U0001F199\n"
        self.assertEqual("close", feature_verdict(body, PATCHES)["verdict"])

    def update(self, body, number=1100, title="[App Update]: x"):
        return update_verdict(number, body, [], PATCHES, title)

    def test_read_the_failure_choices_from_the_form(self):
        options = form_options("Patching this version", FORM)
        self.assertEqual(["Not tried", "Patching fails", "Patching works, the app misbehaves",
                          "Patching works and so does the app"], options)
        self.assertEqual({"patchingfails", "patchingworkstheappmisbehaves", "triedbutpatchingfailed"}, failure_choices(FORM))

    def test_keep_a_failure_on_a_supported_version_open_for_every_failure_choice(self):
        for name, body in (("1028", PROTON_FOSS_UPDATE_1028), ("1048", DUOLINGO_UPDATE_1048)):
            for choice in form_options("Patching this version", FORM):
                failure = choice != "Not tried" and "so does the app" not in choice
                with self.subTest(issue=name, choice=choice):
                    changed = body.replace("Patching fails", choice)
                    result = self.update(changed)
                    self.assertEqual("flag" if failure else "close", result["verdict"])
                    self.assertEqual("" if failure else "completed", result["reason"])

    def test_keep_the_legacy_failure_wording_open(self):
        result = self.update(DUOLINGO_UPDATE_1048.replace("Patching fails", "Tried but patching failed"))
        self.assertEqual("flag", result["verdict"])


if __name__ == "__main__":
    unittest.main()
