#!/usr/bin/env python3
"""python3 feature_request.py <issue-body-file> <patches-list.json> [requests-category]"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from fields import code_span, field, normalize, parse_fields
from replies import REPO

MARKER = "<!-- feature-request-bot -->"
MIN_SIGNAL = 4


def supported_signals(patches_list):
    signals = set()
    for patch in patches_list["patches"]:
        for package in patch.get("compatiblePackages") or []:
            name = package["name"]
            signals |= {normalize(name), normalize(package["packageName"])}
            signals |= {word for word in map(normalize, name.split()) if len(word) >= MIN_SIGNAL}
    return signals


def names_supported_app(app, patches_list):
    typed = normalize(app)
    if sum(c.isalpha() for c in typed) < MIN_SIGNAL:
        return True
    return any(signal in typed or typed in signal for signal in supported_signals(patches_list))


def decide(body, patches_list, category="app-requests"):
    app = field(parse_fields(body), "App")
    if not app or names_supported_app(app, patches_list):
        return {"verdict": "pass", "comment": ""}
    requests = f"https://github.com/{REPO}/discussions/new?category={category}"
    return {
        "verdict": "close",
        "comment": f"{MARKER}\n{code_span(app)} isn't supported by this bundle, so feature requests for it can't be "
                   f"taken here. Patches for a new app start as an [app request]({requests}) in Discussions, where "
                   "others can upvote it. This issue is now closed.",
    }


if __name__ == "__main__":
    body = Path(sys.argv[1]).read_text(encoding="utf-8")
    patches_list = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
    print(json.dumps(decide(body, patches_list, *sys.argv[3:4])))
