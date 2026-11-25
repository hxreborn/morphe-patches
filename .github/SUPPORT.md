# Support

This is a hobby project run by one developer. Requests for new apps go to Discussions, where you can upvote them. Everything about a supported app goes through issues, one issue per topic. Pick the form that fits:

| You want | Open |
| --- | --- |
| Patches for an app not supported yet | [App request](https://github.com/hxreborn/morphe-patches/discussions/new?category=app-requests) in Discussions |
| A supported app has a newer version than the README lists | [App update](https://github.com/hxreborn/morphe-patches/issues/new?template=app_update.yml) |
| Patching would not start (a patch greyed out or Not available), stopped with an error, or the APK would not install | [Patching failed](https://github.com/hxreborn/morphe-patches/issues/new?template=bug_report.yml) |
| The patched app opens but misbehaves | [Patched app broken](https://github.com/hxreborn/morphe-patches/issues/new?template=not_working.yml) |
| A new feature for a supported app | [Feature request](https://github.com/hxreborn/morphe-patches/issues/new?template=feature_request.yml) |
| A question about patching, installing or using the bundle | [Q&A](https://github.com/hxreborn/morphe-patches/discussions/new?category=q-a) in Discussions |

## Recording a log

A crash, or an app stuck on its logo, needs a log of the moment it happens. Record it, reproduce the problem, and attach the file to your issue.

With a PC, run `adb logcat -c`, reproduce the problem, then run `adb logcat -d > log.txt` and attach `log.txt`.

Without a PC, use [LogFox](https://github.com/F0x1d/LogFox) ([F-Droid](https://f-droid.org/packages/com.f0x1d.logfox)):

1. Give LogFox access to the system log through root, [Shizuku](https://shizuku.rikka.app/), or once from a PC with `adb shell pm grant com.f0x1d.logfox android.permission.READ_LOGS`. Without one of these the log comes out empty.
2. Start recording in LogFox.
3. Force-stop the patched app, open it and reproduce the problem. For an app that won't open, leave it on its logo for a minute.
4. Stop recording, export it to a ZIP and attach the ZIP to your issue.

Check the exported file isn't 0 bytes before posting. A log can contain email addresses, account names or message text, so look through it and remove anything private first.

## Good to know

- There are no ETAs. Requests and issues are handled when there's time, in no fixed order, and pinging or mentioning the developer doesn't move them up. Progress is posted on the request or issue itself.
- One app per request. Upvote an existing request instead of opening another one for the same app. Each person can have 3 requests open at a time.
- Each app update covers one app version. A newer version gets its own app update request.
- Closed issues are locked automatically after about a week without activity. Anything new goes in a new issue.
- A bot checks every new or edited issue and request. It points out anything missing, links bundles that already patch the app, and closes reports it can't act on.
- Features the server enforces can't be patched.
- Patches from other bundles are handled in their own repositories. Find them at [morphe-patches.software](https://morphe-patches.software/#apps).
