from datetime import datetime, timedelta, timezone

from unanswered_pings import PING_MARKER, unanswered_ping

NOW = datetime(2026, 11, 16, 12, tzinfo=timezone.utc)
OWNER = "developer"


def post(author, when, body="", replies=None):
    return {"author": {"login": author}, "createdAt": when.isoformat(), "body": body, "replies": replies or []}


def request(*comments, labels=("app request",), **extra):
    return {"number": 489, "state": "OPEN", "author": {"login": "reporter"},
            "createdAt": (NOW - timedelta(days=17)).isoformat(), "updatedAt": NOW.isoformat(),
            "labels": [{"name": label} for label in labels], "comments": list(comments), **extra}


PING = "Hey @developer \r\nCould you at least provide a reply? "
BOT_PING_REPLY = PING_MARKER + "\nThere's no ETA for this request."

assert unanswered_ping(request(post("reporter", NOW - timedelta(hours=4), PING)), OWNER, NOW)
assert unanswered_ping(request(post("other", NOW - timedelta(days=60), "any update?")), OWNER, NOW)
assert unanswered_ping(request(post("github-actions", NOW - timedelta(days=5), "<!-- app-request-bot -->\nok"),
                               post("reporter", NOW - timedelta(hours=1), "bump")), OWNER, NOW)
assert unanswered_ping(request(post("other", NOW - timedelta(days=2), "Works?", replies=[post("reporter", NOW, "any update?")])), OWNER, NOW)
print("Unanswered pings get the reply")

assert not unanswered_ping(request(post("reporter", NOW - timedelta(hours=4), PING), post("github-actions", NOW, BOT_PING_REPLY)), OWNER, NOW)
assert not unanswered_ping(request(post("github-actions", NOW - timedelta(days=3), BOT_PING_REPLY),
                                   post("reporter", NOW, "any update?")), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW - timedelta(hours=4), PING), post(OWNER, NOW, "Not yet")), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW - timedelta(hours=4), PING, replies=[post(OWNER, NOW, "Not yet")])), OWNER, NOW)
assert not unanswered_ping(request(post(OWNER, NOW, "any update?")), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW, "Here are the screenshots of the paywall")), OWNER, NOW)
assert not unanswered_ping(request(), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW, PING), state="CLOSED"), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW, PING), labels=("app request", "bot-wrong")), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW, PING), commentsComplete=False), OWNER, NOW)
assert not unanswered_ping(request(post("reporter", NOW - timedelta(days=8), PING), labels=("app request", "other bundle")), OWNER, NOW)
print("Answered, closed, incomplete and timed-out requests get no reply")
