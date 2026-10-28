#!/usr/bin/env python3
"""python3 generate_patches_readme.py <owner/repo> <branch> [patches-list.json] [README.md]"""

import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path


if len(sys.argv) < 3:
    print("Usage: generate_patches_readme.py <owner/repo> <branch> [json] [readme]")
    sys.exit(1)

repo_full   = sys.argv[1]
branch      = sys.argv[2]
json_path   = Path(sys.argv[3]) if len(sys.argv) > 3 else Path("patches-list.json")
readme_path = Path(sys.argv[4]) if len(sys.argv) > 4 else Path("README.md")


if "/" not in repo_full:
    raise ValueError(f"Invalid repo format: {repo_full} (expected owner/repo)")

owner, repo = repo_full.split("/", 1)


with open(json_path, encoding="utf-8") as f:
    data = json.load(f)


by_pkg = {}
universal = {}

for patch in data["patches"]:
    cp = patch.get("compatiblePackages")
    if not cp:
        if patch["name"] not in universal:
            universal[patch["name"]] = patch
        continue
    for pkg_entry in cp:
        pkg  = pkg_entry["packageName"]
        name = pkg_entry.get("name") or pkg
        if pkg not in by_pkg:
            by_pkg[pkg] = {"name": name, "patches": {}, "targets": []}
        for target in pkg_entry.get("targets") or []:
            if target not in by_pkg[pkg]["targets"]:
                by_pkg[pkg]["targets"].append(target)
        if patch["name"] not in by_pkg[pkg]["patches"]:
            by_pkg[pkg]["patches"][patch["name"]] = patch


def slug(text):
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", text.lower())).strip("-")


PATCHES_SRC  = Path("patches/src/main/kotlin")
COMPAT_FILE  = PATCHES_SRC / "app/morphe/patches/shared/compat/AppCompatibilities.kt"


def build_source_index():
    if not PATCHES_SRC.is_dir():
        return {}

    compat = {}
    if COMPAT_FILE.is_file():
        text = COMPAT_FILE.read_text(encoding="utf-8")
        for const, body in re.findall(r"val (\w+) = Compatibility\((.*?)\n    \)", text, re.DOTALL):
            pkg = re.search(r'packageName = "([^"]+)"', body)
            if pkg:
                compat[const] = pkg.group(1)

    index = {}
    for path in PATCHES_SRC.rglob("*.kt"):
        if path == COMPAT_FILE:
            continue
        text = path.read_text(encoding="utf-8")
        names = re.findall(r'^\s{4}name = "([^"]+)"', text, re.MULTILINE)
        if not names:
            continue
        consts = re.findall(r"compatibleWith\(\s*AppCompatibilities\.(\w+)", text)
        pkgs = [compat.get(c) for c in consts] or [None]
        for name in names:
            for pkg in pkgs:
                index.setdefault((name, pkg), path.as_posix())
    return index


SOURCES = build_source_index()


def patch_cell(patch, pkg, pid):
    source = SOURCES.get((patch["name"], pkg))
    label = f'[{patch["name"]}]({source})' if source else patch["name"]
    return f'<a id="{pid}"></a>{label}'


def patches_table(patches, app_slug, pkg=None):
    patches = sorted(patches, key=lambda x: x["name"])
    has_options = any(p.get("options") for p in patches)

    if has_options:
        rows = ["| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |", "|----------|----------------|-----------|"]
    else:
        rows = ["| 💊&nbsp;Patch | 📜&nbsp;Description |", "|----------|----------------|"]

    for p in patches:
        pid = f"{app_slug}-{slug(p['name'])}"
        cell = patch_cell(p, pkg, pid)
        desc = (p.get("description") or "").replace("\n", "<br>")
        if not has_options:
            rows.append(f"| {cell} | {desc} |")
            continue
        options = p.get("options") or []
        parts = [opt.get("title") or opt.get("key") or "" for opt in options]
        opts_cell = "<br>".join(f"• {t}" for t in parts)
        rows.append(f"| {cell} | {desc} | {opts_cell} |")
    return "\n".join(rows)


ANY_VERSION = "Any version"


def versions_table(targets):
    columns = []
    for t in targets:
        ver = t["version"] or ANY_VERSION
        label = f"🧪&nbsp;{ver}" if t.get("isExperimental") else ver
        if label not in [c[0] for c in columns]:
            columns.append((label, (t.get("description") or "").replace("\n", "<br>")))

    if len(columns) > 1:
        columns = [c for c in columns if c[0] != ANY_VERSION]

    if not columns:
        return ""

    rows = [
        "| " + " | ".join(label for label, _ in columns) + " |",
        "| " + " | ".join(":---:" for _ in columns) + " |",
    ]
    if any(desc for _, desc in columns):
        rows.append("| " + " | ".join(desc for _, desc in columns) + " |")

    return "\n".join(rows)


ICONS = {
    "com.thesortdapp.sortd": "albo.png",
    "com.hardcodedjoy.iototoy": "iototoy.png",
    "com.oneletters.audiobooksai": "audiobookunlimited.png",
    "ir.ilmili.telegraph": "graphmessenger.png",
    "all.in.one.calculator": "allinonecalculator.png",
    "com.allformatplayer.streamvideoplayer": "allvideoplayer.png",
    "com.getaloa.app": "aloa.png",
    "psyberia.alpinequest.free": "alpinequest.png",
    "com.atlogis.atlomaps": "atlomaps.png",
    "com.shervinkoushan.anyTracker": "anytracker.png",
    "com.androholic.wallpapersdepthpix": "depthpix.png",
    "flar2.devcheck": "devcheck.png",
    "com.ferdinandobermeier.focusmate": "focusnow.png",
    "com.adithya.memoneet": "memoneet.png",
    "com.halalfire.medicneet": "medicneet.png",
    "com.lernr.app": "neetprep.png",
    "com.gooded.essential": "neetprepessential.png",
    "com.withopal.opal": "opal.png",
    "com.flyersoft.moonreader": "moonreader.png",
    "com.burton999.notecal": "calcnote.png",
    "org.withouthat.acalendar": "acalendar.png",
    "com.quickcursor": "quickcursor.png",
    "com.biobird.exvee": "biobird.png",
    "com.stresscodes.wallp": "walp.png",
    "com.fengeon.resprite_desktop": "resprite.png",
    "wp.wattpad": "wattpad.png",
    "com.wallmaa.minimal.wallpapers": "wallmaa.png",
    "com.spocky.projengmenu": "projectivy.png",
    "app.privacynotes": "privacynotes.png",
    "com.myvitale.forus": "forus.png",
    "com.psiphon3.subscription": "psiphon.png",
    "com.hypenet.focused": "blockit.png",
    "im.thebot.messenger": "botim.png",
    "com.klevico.monet": "monet.png",
    "com.pengyou.cloneapp": "cloneapp.png",
    "com.athan": "athan.png",
    "com.cardfeed.video_public": "publicvideos.png",
    "com.michaldrabik.showly2": "showly.png",
    "co.steezy.app": "steezy.png",
    "com.storywise.storywise": "storywise.png",
    "com.sqcom.shangqing": "sqcom.png",
    "ch.protonmail.android": "protonmail.png",
    "proton.android.pass": "protonpass.png",
    "proton.android.authenticator": "protonauthenticator.png",
    "com.audible.application": "audible.png",
    "app.symfonik.music.player": "symfonium.png",
    "ai.perplexity.app.android": "perplexity.png",
    "com.poe.android": "poe.png",
    "com.thetrainline": "trainline.png",
    "org.readera": "readera.png",
    "com.pleco.chinesesystem": "pleco.png",
    "com.kick.mobile": "kick.png",
    "com.etsy.android": "etsy.png",
    "com.gamma.scan": "qrscanner.png",
    "com.cxinventor.file.explorer": "cx.png",
    "com.bereal.ft": "bereal.png",
    "com.gxnet.castle.indiatv": "castlefortv.png",
    "com.external.castle": "castle.png",
    "com.streetwriters.notesnook": "notesnook.png",
    "com.orion.notein.global": "notein.png",
    "io.raindrop.raindropio": "raindrop.png",
    "app.rubberbands.fit": "rubberbands.png",
    "io.lightray.photone": "photone.png",
    "photo.editor.photoeditor.photoeditorpro": "photoeditorpro.png",
    "com.community.oneroom": "moviebox.png",
    "com.community.mbox.tv": "movieboxtv.png",
    "com.musixmatch.android.lyrify": "musixmatch.png",
    "com.myscript.math": "myscriptmath.png",
    "com.myscript.nebo": "myscriptnotes.png",
    "com.gstarmc.android": "dwgfastview.png",
    "com.mchutov.Quranify": "quranify.png",
    "com.reworewo.prayertimes": "prayertimes.png",
    "com.sry.rateglance": "rateglance.png",
    "com.risesci.nyx": "risesleep.png",
    "ipnossoft.rma.free": "bettersleep.png",
    "com.handmark.expressweather": "oneweather.png",
    "com.free.vpn.super.hotspot.open": "vpnsuper.png",
    "de.klassikradio.app": "klassikradio.png",
    "dev.vodik7.atvtools": "atvtools.png",
    "com.soulbreakers.mymoveset": "mymoveset.png",
    "com.realme.link": "realmelink.png",
    "ringtonemaker.musiccutter.customringtones.freeringtonemaker": "ringtonemaker.png",
    "com.vpn.free.hotspot.secure.vpnify": "vpnify.png",
    "ch.protonvpn.android": "protonvpn.png",
    "com.alokmandavgane.hinducalendar": "hinducalendar.png",
    "com.outscar.bd.bongcal": "banglacalendar.png",
    "com.darinsoft.vimo": "vllo.png",
    "videoeditor.videomaker.videoeditorforyoutube": "videoeditor.png",
    "com.dubox.drive": "terabox.png",
    "com.liori.echogram": "echogram.png",
    "com.hapibits.soundlift": "echoequalizer.png",
    "com.imo.android.imoim": "imo.png",
    "com.internet.speed.meter.lite": "internetspeedmeterlite.png",
    "com.ledblinker": "ledblinker.png",
    "com.lingodeer": "lingodeer.png",
    "me.proton.android.lumo": "lumo.png",
    "com.fddb": "fddb.png",
    "com.performance.meshview": "faststlviewer.png",
    "de.prosiebensat1digital.seventv": "joyn.png",
    "at.zappn": "joyn-austria.png",
    "ru.iiec.cxxdroid": "cxxdroid.png",
    "ru.iiec.jvdroid": "jvdroid.png",
    "ru.iiec.pydroid3": "pydroid.png",
    "com.yunyi.smartcamera": "yiiot.png",
    "com.greenstone.pocketwhip": "pocketwhip.png",
    "apps.automan.blurwallpaper": "blurwall.png",
    "com.nieruo.healthapp": "catzy.png",
    "tech.coddy.coddyapp": "coddy.png",
    "jp.co.celsys.clipstudiopaint.googleplay": "clipstudiopaint.png",
    "ltd.anti.clink": "clink.png",
    "com.steinberg.cubasisle3": "cubasisle3.png",
    "com.one4studio.one4home": "one4home.png",
    "com.keepa.mobile": "keepa.png",
    "com.hitrolab.audioeditor": "audiolab.png",
    "com.lagguy.wallpapers": "lagguy.png",
    "net.techet.netanalyzerlite.an": "netanalyzer.png",
    "com.radiojavan.androidradio": "radiojavan.png",
    "com.triovent.datingapp": "sheytoon.png",
    "com.serializdmobile": "serializd.png",
    "in.krosbits.musicolet": "musicolet.png",
    "in.krosbits.castplugin": "musicoletcastplugin.png",
    "com.connected2.ozzy.c2m": "connected2.png",
    "com.youdao.hindict": "udictionary.png",
    "com.ufoneselfcare": "uptcl.png",
    "com.generalmagic.magicearth": "magicearth.png",
    "com.lbrc.PeriodCalendar": "periodcalendar.png",
    "com.sygic.aura": "sygic.png",
    "app.threesome.dating": "3way.png",
    "com.omniashare.minishare": "minishare.png",
    "com.naraorganics.nara": "narababy.png",
    "com.jndapp.glass.widgets": "glasswidgets.png",
    "io.yuka.android": "yuka.png",
    "vpn.usa_tap2free": "usavpn.png",
    "india.vpn_tap2free": "vpnindia.png",
    "com.viber.voip": "viber.png",
    "com.telug.keyboard.for.android": "deshtelugu.png",
    "com.fillobotto.mp3tagger": "automatag.png",
}

ICON_DIR = Path(".github/assets/icons")
PLAY_URL = "https://play.google.com/store/apps/details?id={}&hl=en&gl=US"
def icon_file(pkg):
    for filename in (ICONS.get(pkg), f"{pkg}.png"):
        if filename and (ICON_DIR / filename).is_file():
            return filename
    return None


def fetch_play_listing(pkg):
    request = urllib.request.Request(PLAY_URL.format(pkg), headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            page = response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return ("", None) if error.code == 404 else None
    except OSError:
        return None
    category = re.search(r'"applicationCategory":"([A-Z_]+)"', page)
    if not category:
        return None
    icon = re.search(r'"image":"(https://play-lh\.googleusercontent\.com/[^"]+)"', page)
    return (category.group(1), icon.group(1) if icon else None)


def download_icon(url, pkg):
    try:
        with urllib.request.urlopen(f"{url}=s96-cc", timeout=20) as response:
            data = response.read()
    except OSError:
        return
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        (ICON_DIR / f"{pkg}.png").write_bytes(data)


def fetch_missing_icons(pkgs):
    for pkg in pkgs:
        if icon_file(pkg):
            continue
        listing = fetch_play_listing(pkg)
        if listing and listing[1]:
            download_icon(listing[1], pkg)


def icon_img(pkg):
    filename = icon_file(pkg)
    if not filename:
        return "📦&nbsp;"
    return f'<img src=".github/assets/icons/{filename}" width="18" align="top">&nbsp;&nbsp;'


APP_NOTES_DIR = Path(__file__).resolve().parents[1] / "readme-notes"


def app_notes(pkg):
    path = APP_NOTES_DIR / f"{pkg}.md"
    return f"\n\n{path.read_text(encoding='utf-8').strip()}" if pkg and path.is_file() else ""


def spoiler(label, count, targets, tbl, expanded=False, pkg=None):
    noun = "patch" if count == 1 else "patches"
    vtbl = versions_table(targets)
    versions_section = f"**🎯 Supported versions:**\n\n{vtbl}\n\n" if vtbl else ""
    tag = "<details open>" if expanded else "<details>"
    return f"""<a id="{slug(label)}"></a>
{tag}
<summary>{icon_img(pkg)}{label}&nbsp;&nbsp;•&nbsp;&nbsp;{count} {noun}</summary>
<br>

{versions_section}{tbl}{app_notes(pkg)}

</details>"""


GRID_COLUMNS = 4


def patch_count(n):
    return f"{n} {'patch' if n == 1 else 'patches'}"


def grid_cell(label, caption, pkg=None):
    filename = icon_file(pkg) if pkg else None
    icon = f'<img src=".github/assets/icons/{filename}" width="40">' if filename else "🌐"
    return (f'<td align="center" width="{100 // GRID_COLUMNS}%"><a href="#{slug(label)}">{icon}<br>{label}</a>'
            f"<br><sub>{caption}</sub></td>")


def app_grid(cells):
    rows = [cells[i:i + GRID_COLUMNS] for i in range(0, len(cells), GRID_COLUMNS)]
    body = "\n".join("<tr>\n" + "\n".join(row) + "\n</tr>" for row in rows)
    return f'<table>\n{body}\n</table>\n\n## <img src="https://raw.githubusercontent.com/{owner}/{repo}/assets/aqua/icons/full-list.png" height="48" align="absmiddle" alt="Full list"> Full list\n'


def build_content(expanded=False):
    lines = [
        f"> **[v{ver}](https://github.com/{owner}/{repo}/releases/tag/v{ver})**"
        f"&nbsp;&nbsp;•&nbsp;&nbsp;`{branch}`&nbsp;&nbsp;•&nbsp;&nbsp;"
        f"{total} patches total"
    ]
    fetch_missing_icons(list(by_pkg))

    apps = sorted(by_pkg.items(), key=lambda item: item[1]["name"].lower())
    cells = [grid_cell(e["name"], patch_count(len(e["patches"])), pkg) for pkg, e in apps]
    if universal:
        cells.append(grid_cell("Universal", patch_count(len(universal))))
    lines.append("")
    lines.append(app_grid(cells))

    for pkg, entry in apps:
        patches = list(entry["patches"].values())
        label   = entry["name"]
        lines.append(spoiler(label, len(patches), entry["targets"], patches_table(patches, slug(label), pkg), expanded, pkg))
        lines.append("")

    if universal:
        uni_patches = list(universal.values())
        noun = "patch" if len(uni_patches) == 1 else "patches"
        tag  = "<details open>" if expanded else "<details>"
        lines.append(f"""<a id="universal"></a>
{tag}
<summary>🌐&nbsp;Universal&nbsp;&nbsp;•&nbsp;&nbsp;{len(uni_patches)} {noun}</summary>
<br>

{patches_table(uni_patches, "universal")}

</details>""")
        lines.append("")

    return "\n".join(lines)


raw_ver = data["version"]
ver   = raw_ver.lstrip("v")
total = len(data["patches"])

readme = readme_path.read_text(encoding="utf-8")

START_PATTERN = r"<!-- PATCHES_START(?:\s+EXPANDED)?\s*-->"
END_MARKER    = "<!-- PATCHES_END -->"

marker_match = re.search(START_PATTERN, readme)

if not marker_match or END_MARKER not in readme:
    print(build_content(expanded=False))
    sys.stderr.write(
        f"⚠️  Markers <!-- PATCHES_START [EXPANDED] --> / {END_MARKER} not found in {readme_path}. "
        "Printed to stdout instead.\n"
    )
    sys.exit(1)

actual_start = marker_match.group(0)

AUTO_EXPAND_THRESHOLD = 20

expanded = (
    total <= AUTO_EXPAND_THRESHOLD or
    "EXPANDED" in actual_start
)

generated  = build_content(expanded=expanded)

readme = readme.replace("https://morphe.software/add-source?github=xyz-user/xyz-patches", f"https://morphe.software/add-source?github={repo_full}")
readme = readme.replace("https://github.com/xyz-user/xyz-patches", f"https://github.com/{repo_full}")

new_readme = re.sub(
    rf"{START_PATTERN}.*?{re.escape(END_MARKER)}",
    f"{actual_start}\n{generated}\n{END_MARKER}",
    readme,
    flags=re.DOTALL,
)
readme_path.write_text(new_readme, encoding="utf-8")
print(f"✅ Injected patches section into {readme_path} (v{ver}, branch={branch}, {total} patches, expanded={expanded})")
