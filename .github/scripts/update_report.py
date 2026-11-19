from versions import compare


STORE_NAMES = {"play": "Play", "apkmirror": "APKMirror", "uptodown": "Uptodown", "apkpure": "APKPure",
               "aptoide": "Aptoide", "apkcombo": "ApkCombo"}


def store_name(source):
    return STORE_NAMES.get(source, source)


def join_names(names):
    return names[0] if len(names) == 1 else ", ".join(names[:-1]) + " and " + names[-1]


def patch_line(name, check, failed, aborted):
    if not check:
        return f"- [ ] {name} not checked"
    if name in check["applied"]:
        return f"- [x] {name} applies"
    if name in failed:
        return f"- [ ] **{name} fails to apply**"
    return f"- [ ] {name} not reached" if aborted else f"- [ ] {name} not in the checked bundle"


def issue_body(app, record):
    candidate = record["candidate"]
    attempts = record.get("verification", {}).get(candidate, [])
    verified = next((a for a in attempts if a.get("package_ok") and a.get("version_ok")), {})
    check = verified.get("patch_check") or {}
    check = {} if check.get("error") else check
    failed = check.get("failed", [])
    aborted = bool(failed or check.get("exit"))
    patches = [name for name in app.get("patches", []) if name not in check.get("skipped", [])]
    sources = [(name, source) for name, source in app.get("sources", {}).items() if source.get("version")]
    listed = [(name, source) for name, source in sources if compare(source["version"], candidate) == 0]
    newer = [(name, source) for name, source in sources if compare(source["version"], candidate) > 0]
    current_codes = ", ".join(map(str, app["current_version_codes"]))
    codes = f", version code {current_codes} → {verified['version_code']}" if current_codes and verified.get("version_code") else ""
    certificate = verified.get("cert_ok")
    lines = ["### Update", "",
             f"{app['name']} `{app['current']}` → `{candidate}`", "",
             f"Package `{app['package']}`{codes}", "",
             "### Checks", "",
             f"- [x] Version confirmed by {join_names([store_name(name) for name, _ in listed])}" if listed
             else "- [ ] **Version not confirmed by any store**",
             "- [x] Package name matches" if verified else "- [ ] **No APK could be checked**"]
    if verified:
        lines.append("- [x] Signing certificate matches stock" if certificate
                     else "- [ ] **Signing certificate differs from stock**" if certificate is False
                     else "- [ ] Signing certificate not checked")
    lines += [patch_line(name, check, failed, aborted) for name in patches]
    if check and not aborted and all(name in check["applied"] for name in patches):
        lines.append("- [ ] Device check")
    if aborted:
        lines += ["", "### Failure", ""]
        if failed:
            lines += ["| Patch | Error |", "|---|---|"]
            lines += [f"| {name} | `{check.get('reasons', {}).get(name, 'see the patcher log').replace('|', chr(92) + '|')}` |"
                      for name in failed]
        if check.get("tail"):
            log = ["<details open><summary>Patcher log</summary>", "", "```", check["tail"].strip(), "```", "", "</details>"]
            lines += ["", *log] if failed else log
    if listed or newer:
        lines += ["", "### Sources", "", "| Source | Version | Detail |", "|---|---|---|"]
        for name, source in listed:
            detail = "APK downloaded" if name == verified.get("source") else f"updated {source['updated']}" if source.get("updated") else ""
            lines.append(f"| {store_name(name)} | {source['version']} | {detail} |")
        lines += [f"| {store_name(name)} | {source['version']} | newer, not verified |" for name, source in newer]
    return "\n".join(lines) + "\n"
