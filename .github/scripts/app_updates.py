#!/usr/bin/env python3
"""
Daily upstream-version check for every app in patches-list.json.

Version lookup and APK download reuse rushiranpise/patches-tracker (pinned below):
its resolve-apk.sh for APKMirror, Uptodown and APKCombo, and its source-URL discovery.
Google Play, Aptoide and APKPure's direct host are package-keyed and need no discovery.

python3 app_updates.py [--patches-list patches-list.json] [--out out]
                       [--cli morphe.jar --mpp patches.mpp] [--only pkg,pkg] [--issues]
"""

import argparse
import base64
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import zipfile
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import unquote

import requests

TRACKER_REPO = "https://github.com/rushiranpise/patches-tracker"
TRACKER_SHA = "b41567ad88e784f04c8a60f1cd11e4c956d8e57a"
UA = "Mozilla/5.0 (X11; Linux x86_64; rv:108.0) Gecko/20100101 Firefox/108.0"
LOOKUP_TIMEOUT = 120
DOWNLOAD_TIMEOUT = 600
PATCH_TIMEOUT = 900

PRERELEASE = {"alpha", "beta", "rc", "pre", "preview", "dev", "canary", "snapshot"}
BUILD_TAG = re.compile(r"[\s_-]*(?<![a-z0-9])(arm64(-v8a)?|armeabi(-v7a)?|armv7|x86(_64)?|tv|universal)(?![a-z0-9])")
ISSUE_LABEL = "app-update"


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
    while tokens and tokens[-1] == (2, 0, ""):
        tokens.pop()
    return tuple(tokens) + ((1, 0, ""),)


def compare(a, b):
    ka, kb = version_key(a), version_key(b)
    return (ka > kb) - (ka < kb)


def load_inventory(path):
    apps = {}
    for patch in json.loads(Path(path).read_text())["patches"]:
        for compat in patch.get("compatiblePackages") or []:
            app = apps.setdefault(compat["packageName"], {
                "package": compat["packageName"],
                "name": compat.get("name") or compat["packageName"],
                "apk_file_type": compat.get("apkFileType"),
                "signatures": compat.get("signatures") or [],
                "targets": [],
                "patches": [],
            })
            app["patches"].append(patch["name"])
            for target in compat.get("targets") or []:
                codes = sorted(set((target.get("versionCodes") or {}).values()))
                entry = {"version": target.get("version"), "version_codes": codes,
                         "experimental": target.get("isExperimental", False)}
                if entry not in app["targets"]:
                    app["targets"].append(entry)
    for app in apps.values():
        versions = [t["version"] for t in app["targets"] if t["version"]]
        app["any_version"] = not versions
        app["current"] = max(versions, key=version_key) if versions else None
        current = next((t for t in app["targets"] if t["version"] == app["current"]), None)
        app["current_version_codes"] = current["version_codes"] if current else []
    return dict(sorted(apps.items(), key=lambda kv: kv[1]["name"].lower()))


def tracker_dir(path):
    path = Path(path)
    if not (path / "scripts" / "resolve-apk.sh").exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "clone", "-q", TRACKER_REPO, str(path)], check=True)
    subprocess.run(["git", "-C", str(path), "checkout", "-q", TRACKER_SHA], check=True)
    return path


def load_discovery(tracker):
    spec = importlib.util.spec_from_file_location(
        "tracker_discovery", tracker / "scripts" / "generate-config-from-constants.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def resolver_env():
    env = dict(os.environ)
    env.setdefault("TEMP_DIR", str(Path(tempfile.gettempdir()) / "app-updates-resolver"))
    if not env.get("FLARESOLVERR_URL"):
        env["FETCH_RETRIES"] = "0"
    env.setdefault("APKCOMBO_RETRIES", "2")
    return env


def run_resolver(tracker, args, timeout):
    try:
        done = subprocess.run(["bash", str(tracker / "scripts" / "resolve-apk.sh"), *args],
                              capture_output=True, text=True, timeout=timeout, env=resolver_env())
    except subprocess.TimeoutExpired:
        return None, f"timed out after {timeout}s"
    lines = [line.strip() for line in done.stdout.splitlines() if re.search(r"\d", line)]
    if lines and (done.returncode == 0 or args[0] == "latest"):
        return lines[0], None
    tail = [line for line in done.stderr.splitlines() if line.startswith(("[-]", "[!]"))]
    return None, (tail[-1] if tail else f"exit {done.returncode}, no version")[:300]


def aurora_login(ctx, stale=None):
    with ctx["lock"]:
        if not ctx.get("aurora", {}).get("auth") or ctx["aurora"]["auth"] == stale:
            response = requests.get("https://auroraoss.com/api/auth", timeout=30,
                                    headers={"User-Agent": "com.aurora.store-4.6.4"})
            ctx["aurora"] = response.json() if response.ok else {"status": response.status_code}
    if not ctx["aurora"].get("auth"):
        raise RuntimeError(f"Aurora dispenser refused a token (HTTP {ctx['aurora'].get('status')})")
    return ctx["aurora"]


def play_apk(app, ctx):
    with ctx["lock"]:
        slot = ctx["play"].setdefault(app["package"], {"lock": threading.Lock()})
    with slot["lock"]:
        if "facts" in slot:
            return slot["facts"]
        workdir = Path(tempfile.mkdtemp(prefix="play-", dir=ctx["workdir"]))
        try:
            login = aurora_login(ctx)
            for _ in range(2):
                subprocess.run([ctx["apkeep"], "-a", app["package"], "-d", "google-play", "-e", login["email"],
                                "--auth-token", login["auth"], "--accept-tos", "-o", "split_apk=1",
                                str(workdir)], capture_output=True, text=True, timeout=DOWNLOAD_TIMEOUT)
                files = sorted(workdir.rglob("*.apk"))
                if files:
                    break
                login = aurora_login(ctx, stale=login["auth"])
            if not files:
                raise RuntimeError("apkeep downloaded nothing")
            archive = workdir / "play.apks"
            with zipfile.ZipFile(archive, "w") as out:
                for file in files:
                    name = "base.apk" if file.name == f"{app['package']}.apk" else file.name
                    out.write(file, name)
            for file in files:
                file.unlink()
            facts = read_apk(archive, app["package"], workdir)
            facts.update(source="play", path=str(archive))
        except Exception as error:
            facts = {"source": "play", "error": f"{type(error).__name__}: {error}"[:300]}
        slot["facts"] = facts
        return facts


def lookup_play(app, ctx):
    from google_play_scraper import app as play_app
    from google_play_scraper.exceptions import NotFoundError
    try:
        info = play_app(app["package"], lang="en", country="us")
    except NotFoundError:
        return {"error": "not listed"}
    url = f"https://play.google.com/store/apps/details?id={app['package']}"
    version = info.get("version") or ""
    if re.search(r"\d", version):
        return {"version": version, "updated": info.get("lastUpdatedOn"), "url": url}
    if not ctx.get("apkeep"):
        return {"error": f"version hidden ({version or 'empty'})", "updated": info.get("lastUpdatedOn")}
    facts = play_apk(app, ctx)
    if facts.get("error"):
        return {"error": f"version hidden, apkeep: {facts['error']}", "url": url}
    return {"version": facts["version"], "version_code": facts["version_code"],
            "updated": info.get("lastUpdatedOn"), "url": url, "via": "apkeep"}


def lookup_aptoide(app, _ctx):
    url = f"https://ws75.aptoide.com/api/7/listAppVersions?package_name={app['package']}&limit=1"
    data = requests.get(url, timeout=30, headers={"User-Agent": UA}).json()
    items = data.get("list") or (data.get("datalist") or {}).get("list") or []
    item = next((i for i in items if i.get("package") == app["package"]), None)
    if not item:
        return {"error": "not listed"}
    file = item.get("file") or {}
    meta = requests.get(f"https://ws75.aptoide.com/api/7/getAppMeta?package_name={app['package']}"
                        f"&vercode={file.get('vercode')}", timeout=30, headers={"User-Agent": UA}).json()
    meta_file = (meta.get("data") or {}).get("file") or {}
    return {"version": file.get("vername"), "version_code": file.get("vercode"),
            "updated": item.get("modified"), "download": meta_file.get("path") or meta_file.get("path_alt"),
            "url": f"https://{(item.get('store') or {}).get('name', 'apps')}.en.aptoide.com/app"}


def lookup_apkpure(app, _ctx):
    for kind in ("XAPK", "APK"):
        url = f"https://d.apkpure.com/b/{kind}/{app['package']}?version=latest"
        location = requests.get(url, timeout=30, allow_redirects=False,
                                headers={"User-Agent": UA}).headers.get("location", "")
        name = re.search(r"filename=([^&]+)", location)
        found = name and re.search(r"^.*?_([A-Za-z ]*\d.*)_APKPure\.[a-z]+$",
                                   unquote(name.group(1)).replace("+", " "), re.I)
        if not found:
            continue
        code = None
        blob = re.search(r"/(?:APK|XAPK)/([A-Za-z0-9_-]+)\?", location)
        if blob:
            decoded = base64.urlsafe_b64decode(blob.group(1) + "==").decode(errors="replace")
            parts = decoded.rsplit("_", 2)
            if len(parts) == 3 and parts[0] == app["package"] and parts[1].isdigit():
                code = int(parts[1])
        return {"version": found.group(1), "version_code": code, "download": url,
                "url": f"https://apkpure.com/search?q={app['package']}", "format": kind.lower()}
    return {"error": "not listed"}


def scraped_lookup(source):
    def lookup(app, ctx):
        url = ctx["urls"].get(app["package"], {}).get(source)
        if source == "apkcombo":
            url = f"https://apkcombo.com/search/{app['package']}/"
        if not url:
            return {"error": "not listed"}
        version, error = run_resolver(ctx["tracker"], ["latest", source, url], LOOKUP_TIMEOUT)
        return {"version": version, "url": url} if version else {"error": error, "url": url}
    return lookup


SOURCES = {
    "play": lookup_play,
    "apkmirror": scraped_lookup("apkmirror"),
    "uptodown": scraped_lookup("uptodown"),
    "apkpure": lookup_apkpure,
    "aptoide": lookup_aptoide,
    "apkcombo": scraped_lookup("apkcombo"),
}


def apkmirror_url(discovery, package, session, timeout):
    html = discovery.fetch_text(session, discovery.apkmirror_search_url(package), timeout)
    paths = discovery.unique(re.findall(r'href=["\'](/apk/[^"\'/]+/[^"\'/]+/)["\']', html))
    for path in sorted(paths, key=lambda p: bool(re.search(r"android-tv|wear-os|-tv/|automotive", p))):
        url = "https://www.apkmirror.com" + path
        if discovery.apkmirror_page_matches_package(discovery.fetch_text(session, url, timeout), package):
            return url
    return ""


def discover_urls(apps, cache_path, discovery):
    cache = json.loads(cache_path.read_text()) if cache_path.exists() else {}
    session = requests.Session()
    session.headers.update({"User-Agent": UA})
    resolvers = {"apkmirror": lambda package, s, t: (apkmirror_url(discovery, package, s, t),),
                 "uptodown": discovery.resolve_uptodown_url}

    def refresh(package):
        entry = cache.setdefault(package, {})
        for source, resolve in resolvers.items():
            if entry.get(source):
                continue
            try:
                entry[source] = resolve(package, session, 30)[0]
            except Exception as error:
                print(f"[{package}] {source} discovery failed: {error}", file=sys.stderr)

    with ThreadPoolExecutor(max_workers=3) as pool:
        list(pool.map(refresh, apps))
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(json.dumps(cache, indent=2, sort_keys=True) + "\n")
    return cache


def query_sources(app, ctx):
    results = {}
    for name, lookup in SOURCES.items():
        started = time.monotonic()
        try:
            result = lookup(app, ctx)
        except Exception as error:
            result = {"error": f"{type(error).__name__}: {error}"[:300]}
        result["seconds"] = round(time.monotonic() - started, 1)
        results[name] = result
    return results


def build_tool(name):
    found = shutil.which(name)
    if found:
        return found
    for root in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"),
                 str(Path.home() / "Android" / "Sdk")):
        tools = sorted(Path(root or "/nonexistent").glob(f"build-tools/*/{name}"))
        if tools:
            return str(tools[-1])
    return None


def base_apk(path, package, workdir):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if "AndroidManifest.xml" in names:
            return path, "apk"
        apks = [n for n in names if n.endswith(".apk")]
        preferred = [n for n in apks if Path(n).name in ("base.apk", f"{package}.apk")]
        candidates = preferred or [n for n in apks if "config." not in n and "split" not in Path(n).name]
        if not candidates:
            raise ValueError(f"no base apk among {apks[:5]}")
        target = workdir / "base.apk"
        target.write_bytes(archive.read(candidates[0]))
        kind = "apkm" if "info.json" in names else "xapk" if "manifest.json" in names else "apks"
        return target, kind


def read_apk(path, package, workdir):
    apk, kind = base_apk(path, package, workdir)
    if path.suffix != f".{kind}":
        path = path.rename(path.with_suffix(f".{kind}"))
        apk = path if apk.suffix == ".bin" else apk
    badging = subprocess.run([build_tool("aapt2") or "aapt2", "dump", "badging", str(apk)],
                             capture_output=True, text=True, timeout=120).stdout
    head = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", badging)
    if not head:
        raise ValueError("aapt2 could not read the manifest")
    abis = re.search(r"native-code: (.+)", badging)
    certs = []
    signer = build_tool("apksigner")
    if signer:
        out = subprocess.run([signer, "verify", "--print-certs", "--max-sdk-version", "36", str(apk)],
                             capture_output=True, text=True, timeout=120).stdout
        certs = re.findall(r"^(?!Source Stamp).*certificate SHA-256 digest: ([0-9a-f]{64})", out, re.M)
    return {"package": head.group(1), "version_code": int(head.group(2)), "version": head.group(3),
            "format": kind, "abis": re.findall(r"'([^']+)'", abis.group(1)) if abis else [],
            "certs": sorted(set(certs)), "size": path.stat().st_size, "path": str(path)}


def download(app, source, result, version, ctx, workdir):
    out = workdir / f"{source}.bin"
    if result.get("download"):
        with requests.get(result["download"], stream=True, timeout=60, headers={"User-Agent": UA}) as response:
            response.raise_for_status()
            with out.open("wb") as handle:
                shutil.copyfileobj(response.raw, handle)
        return out
    _, error = run_resolver(ctx["tracker"], [source, result["url"], version, str(out), "all",
                                              "nodpi anydpi auto", "apk apkm xapk apks"], DOWNLOAD_TIMEOUT)
    if out.exists() and out.stat().st_size:
        return out
    raise RuntimeError(error or "resolver produced no file")


def patch_check(apk, ctx, workdir):
    java = shutil.which("java")
    args = [java, "-jar", ctx["cli"], "patch", "-p", ctx["mpp"], "-f", "--keystore",
            str(workdir / "check.keystore"), "-o", str(workdir / "patched.apk"),
            "-t", str(workdir / "cli-tmp"), str(apk)]
    try:
        done = subprocess.run(args, capture_output=True, text=True, timeout=PATCH_TIMEOUT)
    except subprocess.TimeoutExpired:
        return {"error": f"timed out after {PATCH_TIMEOUT}s"}
    log = done.stdout + done.stderr
    applied = sorted({name.strip() for name in re.findall(r"Applied: (.+)", log)})
    failed = sorted({name.strip() for name in re.findall(r"FAILED: (.+)", log)})
    messages = "\n".join(line for line in log.splitlines() if line.strip() and not line.startswith("\tat "))
    return {"applied": applied, "failed": failed, "exit": done.returncode,
            "tail": "" if done.returncode == 0 else messages[-1500:]}


def judge(app, facts, version, reference):
    facts["package_ok"] = facts["package"] == app["package"]
    facts["version_ok"] = compare(facts["version"], version) == 0
    facts["cert_ok"] = bool(set(facts["certs"]) & reference) if reference and facts["certs"] else None
    return facts


def verify(app, version, sources, ctx):
    reference = set(app["signatures"])
    play = play_apk(app, ctx) if ctx.get("apkeep") else {}
    if not reference and play.get("certs"):
        reference = set(play["certs"])
    attempts = []
    if play.get("version") and compare(play["version"], version) == 0:
        facts = judge(app, {k: v for k, v in play.items() if k != "path"}, version, set(app["signatures"]))
        if ctx.get("cli"):
            facts["patch_check"] = patch_check(Path(play["path"]), ctx, Path(play["path"]).parent)
        return [facts]
    order = [s for s in ("aptoide", "apkpure", "apkmirror", "uptodown", "apkcombo")
             if s in sources and compare(sources[s].get("version"), version) == 0]
    for source in order:
        workdir = Path(tempfile.mkdtemp(prefix=f"verify-{source}-", dir=ctx["workdir"]))
        try:
            facts = read_apk(download(app, source, sources[source], version, ctx, workdir),
                             app["package"], workdir)
            facts["source"] = source
            judge(app, facts, version, reference)
            if ctx.get("cli") and facts["package_ok"] and facts["version_ok"]:
                facts["patch_check"] = patch_check(Path(facts["path"]), ctx, workdir)
            attempts.append(facts)
            if facts["package_ok"] and facts["version_ok"] and facts["cert_ok"] is not False:
                break
        except Exception as error:
            attempts.append({"source": source, "error": f"{type(error).__name__}: {error}"[:300]})
        finally:
            shutil.rmtree(workdir, ignore_errors=True)
    for attempt in attempts:
        attempt.pop("path", None)
    return attempts


def classify(app, sources, ctx):
    found = {name: r["version"] for name, r in sources.items() if r.get("version")}
    record = {"sources": sources, "notes": []}
    if not found:
        record.update(status="failed", candidate=None)
        return record
    if app["any_version"]:
        record.update(status="any-version", candidate=max(found.values(), key=version_key))
        return record

    newer = {}
    for name, version in found.items():
        if compare(version, app["current"]) > 0:
            newer.setdefault(version_key(version), []).append(name)
    behind = sorted(n for n, v in found.items() if compare(v, app["current"]) < 0)
    if behind:
        record["notes"].append("behind current: " + ", ".join(f"{n} {found[n]}" for n in behind))
    if not newer:
        record.update(status="current", candidate=None)
        return record

    candidates = sorted(newer.items(), reverse=True)
    play = found.get("play")
    if play and compare(play, app["current"]) <= 0:
        record["notes"].append(f"play still lists {play}")
    if len(candidates) > 1:
        record["notes"].append("sources disagree: " + "; ".join(
            f"{found[names[0]]} ({', '.join(names)})" for _, names in candidates))

    confirmed = None
    record["verification"] = {}
    for key, names in candidates:
        version = found[names[0]]
        evidence = [f"{len(names)} sources" if len(names) > 1 else None,
                    "play" if "play" in names else None]
        attempts = verify(app, version, sources, ctx)
        record["verification"][version] = attempts
        good = next((a for a in attempts if a.get("package_ok") and a.get("version_ok")
                     and a.get("cert_ok") is not False), None)
        if good:
            cert = ("cert matches" if good["cert_ok"] else
                    "served by play" if good["source"] == "play" else "cert unchecked")
            evidence.append(f"apk from {good['source']} (vc {good['version_code']}, {cert})")
        elif any(a.get("cert_ok") is False for a in attempts):
            record["notes"].append(f"{version}: apk signed by a foreign certificate")
        evidence = [e for e in evidence if e]
        if evidence and not confirmed:
            confirmed = (version, names, evidence)

    if confirmed:
        record.update(status="update", candidate=confirmed[0], confirmed_by=confirmed[2],
                      reported_by=confirmed[1])
    else:
        version = found[candidates[0][1][0]]
        record.update(status="unconfirmed", candidate=version, reported_by=candidates[0][1])
    return record


def readiness(app, record):
    if record["status"] != "update":
        return None
    checks = [a["patch_check"] for a in record.get("verification", {}).get(record["candidate"], [])
              if a.get("patch_check")]
    if not checks:
        return "untested"
    check = checks[0]
    if check.get("error"):
        return "patch check error"
    expected = set(app["patches"])
    applied = set(check["applied"])
    if check["failed"]:
        return f"needs work: {len(check['failed'])} failed"
    if check["exit"]:
        return f"patch run failed (exit {check['exit']})"
    if not expected <= applied:
        return f"needs work: {len(expected - applied)} not applied"
    return "ready"


def render(report):
    groups = [("update", "Newer upstream version confirmed"),
              ("unconfirmed", "Newer version reported by one source only"),
              ("failed", "Source resolution failed"),
              ("any-version", "Patch accepts any version"),
              ("current", "Already current")]
    lines = [f"# App update check {report['generated_at']}", "",
             f"Inventory: `{report['inventory']}` bundle {report['bundle_version']}, "
             f"{len(report['apps'])} apps. Sources: {', '.join(SOURCES)}.", ""]
    for status, title in groups:
        apps = [a for a in report["apps"] if a["status"] == status]
        if not apps:
            continue
        lines += [f"## {title} ({len(apps)})", "",
                  "| App | Package | Ours | Candidate | Evidence | Readiness | Notes |",
                  "|---|---|---|---|---|---|---|"]
        for app in apps:
            found = ", ".join(f"{n} {r['version']}" + (f" ({r['version_code']})" if r.get("version_code") else "")
                              for n, r in app["sources"].items() if r.get("version"))
            if status == "failed":
                found = "; ".join(f"{n}: {r.get('error')}" for n, r in app["sources"].items())
            evidence = "; ".join(app.get("confirmed_by") or []) or found
            cells = [app["name"], f"`{app['package']}`",
                     f"{app['current']} ({','.join(map(str, app['current_version_codes']))})"
                     if app["current"] else "any",
                     app.get("candidate") or "", evidence, app.get("readiness") or "",
                     "; ".join(app["notes"])]
            lines.append("| " + " | ".join(str(c).replace("|", "\\|") for c in cells) + " |")
        lines.append("")
    return "\n".join(lines)


def gh(*args, **kwargs):
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True, **kwargs).stdout


def issue_body(app, record):
    candidate = record["candidate"]
    attempts = record.get("verification", {}).get(candidate, [])
    good = next((a for a in attempts if a.get("package_ok") and a.get("version_ok")), {})
    check = good.get("patch_check") or {}
    step = {"ready": f"retarget with `feat({app['name']}): support {candidate}`",
            "untested": "run the patch check against the released bundle"}.get(
        record["readiness"], "read the patch log below and repair what failed")
    lines = [f"Package: `{app['package']}`",
             f"Our target: {app['current']} ({', '.join(map(str, app['current_version_codes']))})",
             f"Candidate: {candidate}" + (f" ({good['version_code']})" if good.get("version_code") else ""),
             f"Reported by: {', '.join(record['reported_by'])}",
             f"Verification: {'; '.join(record['confirmed_by'])}",
             f"Patch check: {record['readiness']}"
             + (f", applied: {', '.join(check['applied'])}" if check.get("applied") else "")
             + (f", failed: {', '.join(check['failed'])}" if check.get("failed") else ""),
             f"Next step: {step}"]
    if check.get("tail"):
        lines += ["", "```", check["tail"].strip(), "```"]
    return "\n".join(lines) + "\n"


def sync_issues(apps, results):
    gh("label", "create", ISSUE_LABEL, "--force", "--color", "FBCA04",
       "--description", "Opened by the app update check when a newer version is confirmed")
    issues = json.loads(gh("issue", "list", "--label", ISSUE_LABEL, "--state", "all", "--limit", "1000",
                           "--json", "number,title,state"))
    by_title = {issue["title"]: issue for issue in issues}
    for record in results:
        app = apps[record["package"]]
        prefix = f"[App Update]: {app['name']} "
        wanted = prefix + record["candidate"] if record["status"] == "update" else None
        for issue in issues:
            if issue["state"] != "OPEN" or not issue["title"].startswith(prefix):
                continue
            version = issue["title"][len(prefix):]
            if compare(app["current"], version) >= 0:
                gh("issue", "close", str(issue["number"]), "--comment", f"Targeted {app['current']}.")
            elif wanted and compare(record["candidate"], version) > 0:
                gh("issue", "close", str(issue["number"]), "--comment", f"Superseded by {record['candidate']}.")
        if not wanted:
            continue
        existing = by_title.get(wanted)
        if existing and existing["state"] == "OPEN":
            gh("issue", "edit", str(existing["number"]), "--body-file", "-", input=issue_body(app, record))
        elif not existing:
            gh("issue", "create", "--title", wanted, "--label", ISSUE_LABEL, "--body-file", "-",
               input=issue_body(app, record))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--patches-list", default="patches-list.json")
    parser.add_argument("--out", default="app-updates")
    parser.add_argument("--tracker-dir", default=str(Path.home() / ".cache" / "app-updates" / "patches-tracker"))
    parser.add_argument("--url-cache", default=str(Path.home() / ".cache" / "app-updates" / "source-urls.json"))
    parser.add_argument("--cli", help="morphe-cli jar; with --mpp, applies the bundle to verified candidates")
    parser.add_argument("--mpp")
    parser.add_argument("--only", help="comma-separated package names")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--issues", action="store_true",
                        help="open, update and close GitHub issues for confirmed updates through gh")
    parser.add_argument("--workdir", default=str(Path.home() / ".cache" / "app-updates"),
                        help="downloads land here, not on tmpfs")
    args = parser.parse_args()

    apps = load_inventory(args.patches_list)
    if args.only:
        wanted = set(args.only.split(","))
        apps = {k: v for k, v in apps.items() if k in wanted}
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    tracker = tracker_dir(args.tracker_dir)
    ctx = {"tracker": tracker, "cli": args.cli, "mpp": args.mpp,
           "apkeep": shutil.which("apkeep"), "lock": threading.Lock(), "play": {},
           "workdir": Path(tempfile.mkdtemp(prefix="app-updates-", dir=args.workdir))}
    ctx["urls"] = discover_urls(list(apps), Path(args.url_cache), load_discovery(tracker))

    def check(app):
        record = classify(app, query_sources(app, ctx), ctx)
        record["readiness"] = readiness(app, record)
        play = ctx["play"].get(app["package"], {}).get("facts", {})
        if play.get("path"):
            shutil.rmtree(Path(play.pop("path")).parent, ignore_errors=True)
        print(f"{app['name']}: {record['status']} {record.get('candidate') or ''}", file=sys.stderr, flush=True)
        return {**{k: app[k] for k in ("name", "package", "apk_file_type", "signatures", "targets",
                                       "current", "current_version_codes", "patches")}, **record}

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        results = list(pool.map(check, apps.values()))
    shutil.rmtree(ctx["workdir"], ignore_errors=True)

    bundle = json.loads(Path(args.patches_list).read_text()).get("version")
    report = {"generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
              "inventory": args.patches_list, "bundle_version": bundle,
              "tracker": f"{TRACKER_REPO}@{TRACKER_SHA}", "apps": results}
    (out / "app-updates.json").write_text(json.dumps(report, indent=2) + "\n")
    (out / "app-updates.md").write_text(render(report))
    if args.issues:
        sync_issues(apps, results)
    failed = sum(1 for r in results if r["status"] == "failed")
    print(f"{len(results)} apps, {failed} with no source answering", file=sys.stderr)
    return 1 if results and failed > len(results) / 2 else 0


if __name__ == "__main__":
    sys.exit(main())
