import base64
import io
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

FONT_FILE = Path(__file__).resolve().parents[1] / "readme" / "third_party" / "fredoka" / "fredoka-latin.woff2"
WEIGHTS = (500, 600)
STATIC = {weight: instancer.instantiateVariableFont(TTFont(FONT_FILE, recalcTimestamp=False), {"wght": weight}) for weight in WEIGHTS}
CMAP = STATIC[500].getBestCmap()

BADGE_TEXT_X = 66
BADGE_LABEL_SIZE = 23
BADGE_CAPTION_SIZE = 15
ARROW_CLEARANCE_X = 206
ACCENT = '<linearGradient id="g" x1="0" x2="1" y1="0" y2="1"><stop offset="0" stop-color="#2aa7e0"/><stop offset="1" stop-color="#5fd0d8"/></linearGradient>'

TAPE_HEIGHT = 40
TAPE_TEXT_SIZE = 16
TAPE_SPEED = 38
TAPE_WIDTH = 600
TAPE_SEPARATOR = "   •   "
TAPE_PALETTE = {
    "dark": {"fg": "#f0f6fc", "muted": "#9198a1"},
    "light": {"fg": "#1f2328", "muted": "#59636e"},
}


def covered(text):
    return all(ord(char) in CMAP for char in text)


def font_bytes(font):
    buffer = io.BytesIO()
    font.save(buffer)
    return buffer.getvalue()


def embedded_font(weight, text):
    options = subset.Options()
    options.flavor = "woff2"
    options.layout_features = ["kern"]
    subsetter = subset.Subsetter(options)
    subsetter.populate(text=text)
    font = TTFont(io.BytesIO(font_bytes(STATIC[weight])), recalcTimestamp=False)
    subsetter.subset(font)
    font.flavor = "woff2"
    return base64.b64encode(font_bytes(font)).decode()


def text_width(weight, size, text):
    missing = sorted({char for char in text if ord(char) not in CMAP})
    if missing:
        raise ValueError(f"glyphs missing from {FONT_FILE.name}: {missing} in {text!r}")
    font = STATIC[weight]
    return sum(font["hmtx"][CMAP[ord(char)]][0] for char in text) * size / font["head"].unitsPerEm


def xml(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace('"', "&quot;")


def badge_svg(label, caption, icon_png, font_text):
    for line, weight, size in ((label, 600, BADGE_LABEL_SIZE), (caption, 500, BADGE_CAPTION_SIZE)):
        end = BADGE_TEXT_X + text_width(weight, size, line)
        if end > ARROW_CLEARANCE_X:
            raise ValueError(f"badge text {line!r} ends at x={end:.1f}, past the arrow at x={ARROW_CLEARANCE_X}")
    fonts = (f'@font-face{{font-family:L;src:url(data:font/woff2;base64,{embedded_font(600, font_text)}) format("woff2")}}'
             f'@font-face{{font-family:C;src:url(data:font/woff2;base64,{embedded_font(500, font_text)}) format("woff2")}}')
    icon = base64.b64encode(icon_png).decode()
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 64" width="240" height="64" role="img" aria-label="{xml(label)}">
<defs><style>{fonts}</style>{ACCENT}<clipPath id="c"><circle cx="32" cy="32" r="24"/></clipPath></defs>
<rect x="2" y="2" width="236" height="60" rx="30" fill="url(#g)"/>
<circle cx="32" cy="32" r="26" fill="#ffffff"/>
<image href="data:image/png;base64,{icon}" x="8" y="8" width="48" height="48" clip-path="url(#c)"/>
<text x="{BADGE_TEXT_X}" y="30" font-family="L" font-size="{BADGE_LABEL_SIZE}" fill="#ffffff">{xml(label)}</text>
<text x="{BADGE_TEXT_X}" y="50" font-family="C" font-size="{BADGE_CAPTION_SIZE}" fill="#ffffff" fill-opacity="0.95">{xml(caption)}</text>
<path d="M214 24l8 8-8 8" fill="none" stroke="#ffffff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
'''


def tape_runs(items):
    runs = []
    for label, name, detail in items:
        runs.append(("l", label + " "))
        runs.append(("n", name))
        if detail:
            runs.append(("m", " " + detail))
        runs.append(("s", TAPE_SEPARATOR))
    return runs


def tape_svg(items, theme):
    palette = TAPE_PALETTE[theme]
    runs = tape_runs(items)
    weights = {"n": 600, "l": 600, "m": 500, "s": 500}
    cycle = sum(text_width(weights[kind], TAPE_TEXT_SIZE, text) for kind, text in runs)
    copies = int(TAPE_WIDTH // cycle) + 2
    spans = "".join(f'<tspan class="{kind}">{xml(text)}</tspan>' for kind, text in runs)
    lines = "".join(f'<text x="{copy * cycle:.2f}" y="{TAPE_HEIGHT // 2 + 6}" xml:space="preserve">{spans}</text>' for copy in range(copies))
    text = "".join(text for _, text in runs)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {TAPE_WIDTH} {TAPE_HEIGHT}" width="{TAPE_WIDTH}" height="{TAPE_HEIGHT}" role="img">
<defs><style>@font-face{{font-family:T;font-weight:500;src:url(data:font/woff2;base64,{embedded_font(500, text)})}}@font-face{{font-family:T;font-weight:600;src:url(data:font/woff2;base64,{embedded_font(600, text)})}}
text{{font-family:T,sans-serif;font-size:{TAPE_TEXT_SIZE}px;font-weight:500}}.n{{font-weight:600;fill:{palette["fg"]}}}.l{{font-weight:600;fill:#2aa7e0}}.m{{fill:{palette["muted"]}}}.s{{fill:#2aa7e0}}
@keyframes run{{from{{transform:translateX(0)}}to{{transform:translateX(-{cycle:.2f}px)}}}}.run{{animation:run {cycle / TAPE_SPEED:.1f}s linear infinite}}
@media (prefers-reduced-motion:reduce){{.run{{animation:none}}}}</style>
<linearGradient id="accent" x1="0" x2="1"><stop offset="0" stop-color="#2aa7e0"/><stop offset="1" stop-color="#5fd0d8"/></linearGradient>
<linearGradient id="fade" x1="0" x2="1"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".06" stop-color="#fff"/><stop offset=".94" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
<mask id="edges" maskUnits="userSpaceOnUse" x="0" y="0" width="{TAPE_WIDTH}" height="{TAPE_HEIGHT}"><rect x="0" y="0" width="{TAPE_WIDTH}" height="{TAPE_HEIGHT}" fill="url(#fade)"/></mask></defs>
<rect x="0" y="0" width="{TAPE_WIDTH}" height="2" fill="url(#accent)"/><rect x="0" y="{TAPE_HEIGHT - 2}" width="{TAPE_WIDTH}" height="2" fill="url(#accent)"/>
<g mask="url(#edges)"><g class="run">{lines}</g></g>
</svg>
'''
