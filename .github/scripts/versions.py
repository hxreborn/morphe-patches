import re

ABIS = {"arm64", "armeabi", "armv7", "x86", "x86_64"}
PRERELEASE = {"alpha", "beta", "rc", "pre", "preview", "dev", "canary", "snapshot"}
APP_UPDATE_TITLE_PREFIX = "[App Update]: "
APK_DOWNLOAD_WARNING = "Always verify the downloaded APK's SHA-256 against a trusted checksum before patching."
APK_VERSION_URLS = {
    ("com.community.mbox.tv", "1.2.0.0923.web_socialmedia"): (
        "https://apk.aoneroom.com/apk/moviebox/release/1.2.0.0923/"
        "com.community.mbox.tv-release-1.2.0.0923-ws-50040018-h.apk"
    ),
    ("com.gxnet.castle.indiatv", "1.1.5"): (
        "https://download.sxwon.com/CastleTV_v1.1.5_0929_19_30_official.apk"
    ),
    ("app.symfonik.music.player", "14.0.0 TV"): (
        "https://www.apkmirror.com/apk/tolriq/symfonium-music-player-cast-android-tv/"
        "symfonium-music-player-cast-android-tv-14-0-0-tv-release/"
    ),
    ("app.symfonik.music.player", "15.1.0 TV"): (
        "https://www.apkmirror.com/apk/tolriq/symfonium-music-player-cast-android-tv/"
        "symfonium-music-player-cast-android-tv-15-1-0-tv-release/"
    ),
}
BUILD_TAG = re.compile(r"(?<![a-z0-9])(arm64(-v8a)?|armeabi(-v7a)?|armv7|x86(_64)?|tv|universal)(?![a-z0-9])")


def version_key(version):
    text = BUILD_TAG.sub("", (version or "").lower())
    first_digit = re.search(r"\d", text)
    if not first_digit:
        return ()
    tokens = []
    for token in re.findall(r"\d+|[a-z]+", text[first_digit.start():]):
        if token.isdigit():
            tokens.append((2, int(token), ""))
        elif token in PRERELEASE:
            tokens.append((0, 0, token))
        else:
            tokens.append((1, 0, token))
    prerelease_start = next((i for i, token in enumerate(tokens) if token[0] == 0), len(tokens))
    while prerelease_start and tokens[prerelease_start - 1] == (2, 0, ""):
        prerelease_start -= 1
        del tokens[prerelease_start]
    while tokens and tokens[-1] == (2, 0, ""):
        tokens.pop()
    return tuple(tokens) + ((1, 0, ""),)


def compare(a, b):
    ka, kb = version_key(a), version_key(b)
    return (ka > kb) - (ka < kb)


def prerelease_words(version):
    text = (version or "").lower()
    first_digit = re.search(r"\d", text)
    return PRERELEASE & set(re.findall(r"[a-z]+", text[first_digit.start():])) if first_digit else set()


def build_tags(version):
    return sorted(m.group(1).split("-")[0] for m in BUILD_TAG.finditer((version or "").lower()))


def same_variant(version, target):
    ours, theirs = build_tags(version), build_tags(target)
    return not ours or ours == theirs or (not theirs and set(ours) <= ABIS)


def tracked_version(title, app, app_names):
    prefix = f"{APP_UPDATE_TITLE_PREFIX}{app} "
    if not title.startswith(prefix) or any(
        len(other) > len(app) and title.startswith(f"{APP_UPDATE_TITLE_PREFIX}{other} ") for other in app_names
    ):
        return None
    version = title[len(prefix):]
    return version if re.search(r"\d", version) else None


def targets(patches_list):
    apps = {}
    for patch in patches_list["patches"]:
        for package in patch.get("compatiblePackages") or []:
            versions = apps.setdefault(package["name"], set())
            versions.update(t["version"] for t in package.get("targets") or [] if t.get("version"))
    return apps
