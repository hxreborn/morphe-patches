#!/usr/bin/env python3
"""python3 app_request.py <issue-body-file> <bundles.json>"""

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from fields import field, parse_fields

MARKER = "<!-- app-request-bot -->"
STORE_ID = re.compile(r"[?&]id=([A-Za-z][A-Za-z0-9_.]+)")
PACKAGE_PATTERN = re.compile(r"\b[a-z][a-z0-9_]*(?:\.[a-z0-9_]+)+\b", re.I)
NOT_PACKAGES = ("play.google", "github.com", "gitlab.com", "www.", "apkmirror.", "apkpure.")
MAX_LISTED = 5

fields = parse_fields(Path(sys.argv[1]).read_text(encoding="utf-8"))
registry = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))

package = field(fields, "Package name", "Package").strip()
if not package:
    body = "\n".join(fields.values())
    store = STORE_ID.search(body)
    if store:
        package = store.group(1)
    else:
        package = next(
            (m.group(0) for m in PACKAGE_PATTERN.finditer(body)
             if not m.group(0).lower().startswith(NOT_PACKAGES)),
            "",
        )

matches = []
if package:
    for bundle in registry.get("bundles", []):
        if package in (bundle.get("targetApps") or []):
            matches.append({
                "name": bundle.get("name") or bundle["repo"],
                "repo": bundle["repo"],
                "source": bundle.get("source", "github"),
                "stars": bundle.get("stars") or 0,
            })

matches.sort(key=lambda b: -b["stars"])
comment = ""
if matches:
    host = {"github": "https://github.com", "gitlab": "https://gitlab.com"}
    rows = "\n".join(
        f"- [{b['name']}]({host.get(b['source'], 'https://github.com')}/{b['repo']})"
        f"{f' ({b['stars']} stars)' if b['stars'] else ''}"
        for b in matches[:MAX_LISTED]
    )
    extra = len(matches) - MAX_LISTED
    more = f"\n\nand {extra} more." if extra > 0 else ""
    comment = (
        f"{MARKER}\n`{package}` already has patches elsewhere:\n\n{rows}{more}\n\n"
        "Add that bundle as a source in Morphe Manager. If you need something those patches "
        "don't cover, say what."
    )

print(json.dumps({"package": package, "matches": matches, "comment": comment}))
