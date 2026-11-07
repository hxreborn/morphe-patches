from datetime import datetime, timedelta, timezone

from discussion_timeouts import EXEMPT_LABELS, last_owner_activity, timeout_reason
from replies import REPLIES

NOW = datetime(2026, 11, 7, 12, tzinfo=timezone.utc)
OWNER = "developer"


def post(author, when, body="", **extra):
    return {"author": {"login": author}, "createdAt": when.isoformat(), "body": body, **extra}


def request(*labels, created=NOW - timedelta(days=14), comments=None, **extra):
    return {"number": 1, "state": "OPEN", "author": {"login": "reporter"}, "createdAt": created.isoformat(),
            "updatedAt": NOW.isoformat(), "labels": [{"name": label} for label in labels],
            "comments": comments or [], **extra}


def asked(when, replies=None):
    return post("github-actions", when, "<!-- app-request-bot facts=example -->\nMissing details", replies=replies or [])


assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=1))]), OWNER, NOW) == "needs-info"
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=1) + timedelta(seconds=1))]), OWNER, NOW) is None
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=2)), post("other", NOW)]), OWNER, NOW) == "needs-info"
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=2)), post("reporter", NOW)]), OWNER, NOW) is None
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=2), [post("reporter", NOW)])]), OWNER, NOW) is None
assert timeout_reason(request("needs info", comments=[post("reporter", NOW - timedelta(days=3)), asked(NOW - timedelta(days=2))]), OWNER, NOW) == "needs-info"
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=3)), asked(NOW - timedelta(hours=1))]), OWNER, NOW) is None
assert timeout_reason(request("needs info", comments=[asked(NOW - timedelta(days=3)), post(OWNER, NOW)]), OWNER, NOW) is None
assert timeout_reason(request("needs info", updatedAt=(NOW - timedelta(days=2)).isoformat()), OWNER, NOW) == "needs-info"
assert timeout_reason(request("needs info"), OWNER, NOW) is None
assert timeout_reason(request("needs info", commentsComplete=False, comments=[asked(NOW - timedelta(days=2))]), OWNER, NOW) is None
print("One-day reply deadline passes")

assert timeout_reason(request("other bundle"), OWNER, NOW) == "other-bundle"
assert timeout_reason(request("other bundle", created=NOW - timedelta(days=7)), OWNER, NOW) == "other-bundle"
assert timeout_reason(request("other bundle", created=NOW - timedelta(days=7) + timedelta(seconds=1)), OWNER, NOW) is None
assert timeout_reason(request("other bundle", comments=[post("reporter", NOW), post("github-actions", NOW)]), OWNER, NOW) == "other-bundle"
assert timeout_reason(request("other bundle", comments=[post(OWNER, NOW - timedelta(days=7))]), OWNER, NOW) == "other-bundle"
assert timeout_reason(request("other bundle", comments=[post(OWNER, NOW - timedelta(days=6))]), OWNER, NOW) is None
assert timeout_reason(request("other bundle", comments=[post("other", NOW, replies=[post(OWNER, NOW)])]), OWNER, NOW) is None
assert timeout_reason(request("other bundle", editor={"login": OWNER}, lastEditedAt=NOW.isoformat()), OWNER, NOW) is None
assert timeout_reason(request("other bundle", comments=[post("other", NOW - timedelta(days=10), editor={"login": OWNER}, lastEditedAt=NOW.isoformat())]), OWNER, NOW) is None
assert last_owner_activity(request("other bundle", comments=[post("other", NOW)]), OWNER) == NOW - timedelta(days=14)
print("Owner activity and seven-day deadline pass")

for label in EXEMPT_LABELS:
    assert timeout_reason(request("needs info", "other bundle", label, comments=[asked(NOW - timedelta(days=2))]), OWNER, NOW) is None
assert timeout_reason(request("needs info", "other bundle", state="CLOSED"), OWNER, NOW) is None
assert "within 1 day" in REPLIES["request-stale"]()
assert "within 3 days" in REPLIES["issue-needs-info-closed"]()
print("Protected labels and separate issue deadline pass")
