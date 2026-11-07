import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

NEEDS_INFO_TIMEOUT = timedelta(days=1)
OTHER_BUNDLE_TIMEOUT = timedelta(days=7)
EXEMPT_LABELS = {"bot-wrong", "in progress", "no-stale", "P0", "P1", "confirmed"}


def timestamp(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def posts(request):
    for comment in request.get("comments", []):
        yield comment
        yield from comment.get("replies", [])


def last_owner_activity(request, owner):
    dates = [timestamp(request["createdAt"])]
    for item in [request, *posts(request)]:
        if (item.get("author") or {}).get("login") == owner:
            dates.append(timestamp(item["createdAt"]))
        if (item.get("editor") or {}).get("login") == owner and item.get("lastEditedAt"):
            dates.append(timestamp(item["lastEditedAt"]))
    return max(dates)


def timeout_reason(request, owner, now):
    labels = {label["name"] for label in request.get("labels", [])}
    if request["state"] != "OPEN" or labels & EXEMPT_LABELS or not request.get("commentsComplete", True):
        return None
    if "needs info" in labels:
        thread_posts = list(posts(request))
        asked = []
        for post in thread_posts:
            author = (post.get("author") or {}).get("login")
            is_triage_request = (author in {"github-actions", "github-actions[bot]"}
                                 and "<!-- app-request-bot" in post.get("body", ""))
            if author == owner or is_triage_request:
                asked.append(timestamp(post["createdAt"]))
        since = max(asked) if asked else timestamp(request["updatedAt"])
        answered = any((post.get("author") or {}).get("login") == request["author"]["login"]
                       and timestamp(post["createdAt"]) > since for post in thread_posts)
        if not answered and since <= now - NEEDS_INFO_TIMEOUT:
            return "needs-info"
    if "other bundle" in labels and last_owner_activity(request, owner) <= now - OTHER_BUNDLE_TIMEOUT:
        return "other-bundle"
    return None


if __name__ == "__main__":
    requests = json.loads(Path(sys.argv[1]).read_text())
    now = datetime.now(timezone.utc)
    for request in requests:
        reason = timeout_reason(request, sys.argv[2], now)
        if reason:
            print(json.dumps({"number": request["number"], "reason": reason}))
