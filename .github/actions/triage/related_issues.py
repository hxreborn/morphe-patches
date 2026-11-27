import html
import json
import os
import re
import subprocess
import sys
from pathlib import Path

from fields import field, normalize, parse_fields


GENERIC_WORDS = set(
    "add app are bug can does dont error fail failed feature for from have how issue just not now option "
    "patch please request since still the this when will with work working would".split()
)
SAME_MEANING = {
    "backing": "backup", "backups": "backup", "drive": "backup", "restore": "backup", "restoring": "backup",
    "gms": "gmscore", "google": "gmscore", "microg": "gmscore",
}
ERROR_CODE = re.compile(r"\b(?:INSTALL_FAILED_[A-Z0-9_]+|[A-Z][A-Za-z]+(?:Exception|Error))\b")
MAX_RESULTS = 3
TITLE_PREFIX = re.compile(r"^(?:\[[^\]]*\]|(?:bug|feat|question)(?:\([^()]*\))?):\s*", re.I)


def words(text):
    return re.findall(r"[^\W_]+", text.casefold())


def meaning(word):
    return SAME_MEANING.get(word, word)


def suggestions(issue, app, label, repo):
    fields = parse_fields(issue["body"] or "")
    title = TITLE_PREFIX.sub("", issue["title"])
    patch = field(fields, "Patch that breaks it").partition("\u00b7")[2].strip()
    error = ERROR_CODE.search(field(fields, "Error logs", "Debug log"))
    subject = field(fields, "Feature description") if label in {"feature", "enhancement"} else field(fields, "Actual behaviour")
    app_words = words(app)
    patch_words = words(patch)
    excluded = GENERIC_WORDS | set(app_words) | set(patch_words)
    terms = list(dict.fromkeys(word for word in words(f"{title} {subject}") if len(word) >= 3 and word not in excluded))
    term_meanings = list(dict.fromkeys(meaning(term) for term in terms))
    queries = ["", *filter(None, [error[0] if error else "", " ".join(terms[:2]), " ".join(patch_words)])]
    candidates = {}
    search_failed = False
    for query in dict.fromkeys(queries):
        try:
            result = subprocess.run(
                ["gh", "search", "issues", "--repo", repo, "--label", label, "--match", "title",
                 "--limit", "30", "--json", "number,title,state", "--", " ".join([*app_words, query]).strip()],
                capture_output=True, text=True, check=True, timeout=15,
            )
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
            search_failed = True
            continue
        for candidate in json.loads(result.stdout):
            if candidate["number"] >= issue["number"]:
                continue
            candidate_words = words(candidate["title"])
            if not set(app_words) <= set(candidate_words):
                continue
            candidate_meanings = {meaning(word) for word in candidate_words}
            shared = [term for term in term_meanings if term in candidate_meanings]
            has_patch = patch_words and f" {' '.join(patch_words)} " in f" {' '.join(candidate_words)} "
            has_error = error and error[0].casefold() in candidate["title"].casefold()
            reasons = []
            if has_error:
                reasons.append(f"error {error[0]}")
            if has_patch:
                reasons.append(f"patch {patch}")
            if shared:
                reasons.append(f"terms {', '.join(shared[:4])}")
            if has_error or len(shared) >= 2 or (has_patch and (shared or not terms)):
                score = (bool(has_error), len(shared), bool(has_patch), candidate["state"] == "open")
                candidates[candidate["number"]] = (score, candidate, ", ".join(reasons))
    ranked = sorted(candidates.values(), key=lambda entry: (entry[0], -entry[1]["number"]), reverse=True)
    return ranked[:MAX_RESULTS], search_failed


def comment_lines(candidates, repo):
    for _, candidate, _ in candidates:
        number = candidate["number"]
        title = " ".join(candidate["title"].replace("`", "'").split())
        print(f"- [#{number}](https://redirect.github.com/{repo}/issues/{number}) `{title}`")


def main(event_path, manifest_path, label, verdict_path=None, output="summary"):
    issue = json.loads(Path(event_path).read_text())["issue"]
    if verdict_path:
        app = json.loads(Path(verdict_path).read_text())["app"]
    else:
        typed = normalize(field(parse_fields(issue["body"] or ""), "App"))
        manifest = json.loads(Path(manifest_path).read_text())
        app = next((package["name"] for patch in manifest["patches"] for package in patch.get("compatiblePackages") or []
                    if typed and typed in {normalize(package["name"]), normalize(package["packageName"])}), "")
    repo = os.environ["REPO"]
    if not app:
        return
    candidates, search_failed = suggestions(issue, app, label, repo)
    if output == "comment":
        if search_failed and not candidates:
            sys.exit(1)
        comment_lines(candidates, repo)
        return
    if search_failed:
        print("\nSome related issue searches failed.")
    if candidates:
        print("\n### Possible duplicates\n")
    for _, candidate, reason in candidates:
        number = candidate["number"]
        title = html.escape(candidate["title"])
        print(f"- [#{number}](https://github.com/{repo}/issues/{number}) <code>{title}</code> (<code>{html.escape(reason)}</code>)")


if __name__ == "__main__":
    main(*sys.argv[1:])
