import json
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts"))
from discussion_timeouts import posts, timeout_reason, timestamp
from issue_comment import PING_MARKER, is_status_ping, ping_reply

BOTS = {"github-actions", "github-actions[bot]"}


def unanswered_ping(request, owner, now):
    labels = {label["name"] for label in request.get("labels", [])}
    if request["state"] != "OPEN" or "bot-wrong" in labels or not request.get("commentsComplete", True):
        return False
    if timeout_reason(request, owner, now):
        return False
    thread = sorted(posts(request), key=lambda post: timestamp(post["createdAt"]))
    if any(PING_MARKER in (post.get("body") or "") for post in thread
           if (post.get("author") or {}).get("login") in BOTS):
        return False
    people = [post for post in thread if (post.get("author") or {}).get("login") not in BOTS]
    if not people:
        return False
    last = people[-1]
    return last["author"]["login"] != owner and is_status_ping(last.get("body") or "")


if __name__ == "__main__":
    requests = json.loads(Path(sys.argv[1]).read_text())
    now = datetime.now(timezone.utc)
    for request in requests:
        if unanswered_ping(request, sys.argv[2], now):
            print(json.dumps({"number": request["number"], "body": ping_reply(request)}))
