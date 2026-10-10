#!/usr/bin/env python3

import base64
import datetime
import shutil
import struct
import sys
from pathlib import Path


SCRIPTS = Path(__file__).resolve().parent
FONT_FILE = SCRIPTS.parent / "assets" / "fonts" / "fredoka-semibold-subset.woff2"
SUBTITLE_FONT_FILE = SCRIPTS.parent / "assets" / "fonts" / "fredoka-regular-subset.woff2"
THEMES = {"light": "#1f2328", "dark": "#f0f6fc"}
SUBTITLE = {"light": "#59636e", "dark": "#9198a1"}


def png_size(path):
    with open(path, "rb") as f:
        header = f.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"{path} is not a PNG")
    return struct.unpack(">II", header[16:24])


def data_uri(path):
    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode()


def pick_of_the_day(folder, day):
    pictures = sorted(folder.glob("*.png"))
    if not pictures:
        raise SystemExit(f"no PNG pictures in {folder}")
    return pictures[day % len(pictures)]


def strip_svg(picture, mode):
    width, height = 1200, 280
    pic_width, pic_height = png_size(picture)
    fig_height = height
    fig_width = round(pic_width * fig_height / pic_height)
    if fig_width > 340:
        fig_width = 340
        fig_height = round(pic_height * fig_width / pic_width)
    font = base64.b64encode(FONT_FILE.read_bytes()).decode()
    subtitle_font = base64.b64encode(SUBTITLE_FONT_FILE.read_bytes()).decode()
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}" role="img" aria-label="hxreborn&#8217;s patches">
<defs><style>@font-face{{font-family:Wordmark;src:url(data:font/woff2;base64,{font}) format("woff2")}}@font-face{{font-family:Subtitle;src:url(data:font/woff2;base64,{subtitle_font}) format("woff2")}}</style>
<linearGradient id="accent" x1="0" x2="1"><stop offset="0" stop-color="#2aa7e0"/><stop offset="1" stop-color="#5fd0d8"/></linearGradient></defs>
<image href="{data_uri(picture)}" x="{350 - fig_width}" y="{height - fig_height}" width="{fig_width}" height="{fig_height}"/>
<text x="380" y="{height // 2}" font-family="Wordmark" font-size="86" fill="{THEMES[mode]}">hxreborn&#8217;s <tspan fill="url(#accent)">patches</tspan></text>
<text x="384" y="{height // 2 + 62}" font-family="Subtitle" font-size="40" fill="{SUBTITLE[mode]}">Android app patches for use with Morphe</text>
</svg>
"""


def main():
    if len(sys.argv) < 2:
        raise SystemExit("Usage: generate_banner.py <assets-dir>")
    assets = Path(sys.argv[1])
    day = datetime.date.today().toordinal()
    top = pick_of_the_day(assets / "aqua" / "top", day)
    bottom = pick_of_the_day(assets / "aqua" / "bottom", day)
    for mode in THEMES:
        (assets / f"banner-{mode}.svg").write_text(strip_svg(top, mode), encoding="utf-8")
    shutil.copyfile(bottom, assets / "request.png")
    print(f"banner: {top.name}; request.png: {bottom.name}")


if __name__ == "__main__":
    main()
