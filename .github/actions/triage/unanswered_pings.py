import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "scripts"))
from discussion_timeouts import posts, timeout_reason, timestamp
from issue_comment import PING_MARKER, is_status_ping, ping_reply

BOTS = {"github-actions", "github-actions[bot]"}
PING_REPLY_WINDOW = timedelta(days=2)


def unanswered_ping(request, owner, now):
    labels = {label["name"] for label in request.get("labels", [])}
    if request["state"] != "OPEN" or "bot-wrong" in labels or not request.get("commentsComplete", True):
        return None
    if timeout_reason(request, owner, now):
        return None
    thread = sorted(posts(request), key=lambda post: timestamp(post["createdAt"]))
    if any(PING_MARKER in (post.get("body") or "") for post in thread
           if (post.get("author") or {}).get("login") in BOTS):
        return None
    people = [post for post in thread if (post.get("author") or {}).get("login") not in BOTS]
    if not people:
        return None
    last = people[-1]
    if now - timestamp(last["createdAt"]) > PING_REPLY_WINDOW:
        return None
    if last["author"]["login"] == owner or not is_status_ping(last.get("body") or ""):
        return None
    return last


def thread_id(request, post):
    for comment in request.get("comments", []):
        if comment is post or any(reply is post for reply in comment.get("replies", [])):
            return comment["id"]
    raise LookupError(f"#{request['number']}: ping {post.get('url')} is in no comment thread")


if __name__ == "__main__":
    requests = json.loads(Path(sys.argv[1]).read_text())
    now = datetime.now(timezone.utc)
    for request in requests:
        ping = unanswered_ping(request, sys.argv[2], now)
        if ping:
            print(json.dumps({"number": request["number"], "replyTo": thread_id(request, ping),
                              "url": ping.get("url"), "body": ping_reply(request)}))
