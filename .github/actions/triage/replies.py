#!/usr/bin/env python3
"""python3 replies.py <reply> [name=value ...]"""

import os
import re
import sys

REPO = os.environ.get("GITHUB_REPOSITORY") or os.environ.get("REPO") or "hxreborn/morphe-patches"
NEW_ISSUE = f"https://github.com/{REPO}/issues/new?template="
FORMS = {
    "bug_report": ("Patching failed", "patching won't start, stops with an error or the APK won't install"),
    "not_working": ("Patched app broken", "the app crashes or a patch does nothing"),
    "feature_request": ("Feature request", "another patch for this app"),
    "app_update": ("App update request", "a newer app version"),
}


def form_list(*templates):
    return "\n".join(f"- [{FORMS[t][0]}]({NEW_ISSUE}{t}.yml): {FORMS[t][1]}" for t in templates)


BROKEN_FORMS = form_list("bug_report", "not_working")
ALL_FORMS = form_list(*FORMS)
APK_SOURCES = ("[APKMirror](https://www.apkmirror.com/), [APKPure](https://apkpure.com/), "
               "[Uptodown](https://www.uptodown.com/) or [APKCombo](https://apkcombo.com/)")
STAR = f"A ⭐ on the [repo](https://github.com/{REPO}) helps more people find these patches."
MISSING_HEADER = "This report is missing information:"
MISSING_FOOTER = "Please edit the issue to add it. It closes automatically after 7 days without activity."


def release_link(tag):
    return f"[{tag}](https://github.com/{REPO}/releases/tag/{tag})"


def bundle(version):
    return f"Bundle {release_link(f'v{version}')}"


def version_order(version):
    return [int(part) if part.isdigit() else part for part in re.split(r"(\d+)", version)]


def code_list(versions):
    items = [f"`{v}`" for v in sorted(versions, key=version_order)]
    return " and ".join([", ".join(items[:-1]), items[-1]]) if len(items) > 1 else "".join(items)


def prerelease_request(app, link):
    return f"{app} has been added in the pre-release {link}. Please try it and reply whether it works. " \
           "This discussion closes when it reaches a stable release."


def updated(thing, consequence=""):
    return f"Thanks for the update, this {thing} has everything it needs now. The earlier note no longer applies{consequence}."


def missing(lines):
    return "\n\n".join([MISSING_HEADER, "\n".join(f"- {line}" for line in lines), MISSING_FOOTER])


REPLIES = {
    "star": lambda: STAR,
    "request-passes": lambda: updated("request", ", and the request is open for votes"),
    "report-passes": lambda: updated("report"),
    "report-reopened": lambda: updated("report", ", and the issue is open again"),
    "update-passes": lambda: updated("request"),
    "request-stale": lambda: ("This discussion is now closed because the missing details weren't added within 7 days. "
                              "Feel free to open a new request with them."),
    "issue-stale": lambda: "This issue has had no activity for 30 days. It closes automatically in 7 days unless someone comments.",
    "issue-stale-closed": lambda: ("This issue is now closed after 37 days without activity. "
                                   f"If it still happens, please open a new issue:\n\n{ALL_FORMS}"),
    "issue-needs-info-closed": lambda: ("This issue is now closed because the missing information wasn't added within 7 days. "
                                        f"Feel free to open a new issue with it:\n\n{ALL_FORMS}"),
    "related": lambda issues: ("These issues may be about the same problem:\n\n"
                               f"{issues}\n\nIf one of them matches, please comment there and close this one. Thanks!"),
    "shipped": lambda app, tag: (f"{app} has been added in {release_link(tag)}. This discussion is now closed. "
                                 f"For anything else about {app}, open an issue:\n\n{ALL_FORMS}\n\n{STAR}"),
}


if __name__ == "__main__":
    values = dict(arg.split("=", 1) for arg in sys.argv[2:])
    print(REPLIES[sys.argv[1]](**values))
