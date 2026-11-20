#!/usr/bin/env python3

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
README = ROOT / "README.md"
ASSETS_URL = "https://raw.githubusercontent.com/hxreborn/morphe-patches/assets/"
ASSETS_REF = "origin/assets"
IMAGE_ATTRIBUTE = re.compile(r"""\b(src|srcset)\s*=\s*(["'])(.*?)\2""", re.IGNORECASE | re.DOTALL)
MARKDOWN_IMAGE = re.compile(r"!\[[^\]]*\]\(<?([^)\s>]+)")


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)


def image_sources(markdown):
    for name, _, value in IMAGE_ATTRIBUTE.findall(markdown):
        candidates = value.split(",") if name.lower() == "srcset" else [value]
        yield from (candidate.split()[0] for candidate in candidates if candidate.strip())
    yield from MARKDOWN_IMAGE.findall(markdown)


def is_blob(spec):
    result = git("cat-file", "-t", spec)
    return result.returncode == 0 and result.stdout.strip() == "blob"


sources = {source.split("#")[0].split("?")[0] for source in image_sources(README.read_text(encoding="utf-8"))}
relative = sorted(source for source in sources if "://" not in source)
assets = sorted(source.removeprefix(ASSETS_URL) for source in sources if source.startswith(ASSETS_URL))

untracked = [path for path in relative if git("ls-files", "--", path).stdout.strip() != path or not (ROOT / path).is_file()]
assert not untracked, f"README images not tracked by git: {untracked}"

if git("rev-parse", "--verify", "--quiet", ASSETS_REF).returncode == 0:
    unpublished = [path for path in assets if not is_blob(f"{ASSETS_REF}:{path}")]
    assert not unpublished, f"README images missing from {ASSETS_REF}: {unpublished}"
    print(f"PASS {len(relative)} tracked and {len(assets)} published README images")
else:
    print(f"PASS {len(relative)} tracked README images, {ASSETS_REF} not fetched so {len(assets)} assets images unchecked")
