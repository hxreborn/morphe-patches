import io
import json
import os
import tempfile
import unittest
from unittest.mock import patch

import app_request
import discussion_ai


BODY = """### App

MyGate

### Package name

com.mygate.user

### Requested features

The currently available patches are no longer maintained and do not work correctly with the latest version of the app.

<img width="842" height="948" src="https://github.com/user-attachments/assets/example" />

Hide Ads: Remove the sponsored cards from Community Posts and the Notice Board.

Unlock Premium/VIP: Bypass the subscription check to enable premium features.

### Acknowledgements

- [X] I searched other requests.
"""

ISSUE = {"number": 1067, "title": "MyGate", "body": BODY, "state": "OPEN",
         "author": {"login": "requester"}, "comments": [
             {"author": {"login": "requester"}, "body": "I tested the current patch and ads still appear. VIP is not unlocked."},
             {"author": {"login": "github-actions"}, "body": "Earlier bot reply"},
         ]}

REGISTRY = {"bundles": [{"repo": "other/patches", "name": "Other", "source": "github",
                         "targetApps": ["com.mygate.user"], "patches": [
                             {"name": "Hide ads", "compatiblePackagesKey": 0},
                             {"name": "Unlock VIP", "compatiblePackagesKey": 0},
                         ]}], "compatibilities": [[{"packageName": "com.mygate.user"}]]}

MODEL_RESULT = {"features": ["Hide Ads", "Unlock Premium/VIP"], "uncovered": [], "reported_broken": True}


class DiscussionReviewTests(unittest.TestCase):
    def test_prose_and_images_are_not_feature_names(self):
        self.assertEqual([], app_request.requested_features(BODY))

    def test_review_rejects_hallucinated_or_html_quotes(self):
        source = app_request.field(app_request.parse_fields(BODY), "Requested features")
        for quote in ("Remove watermarks", '<img width="842">'):
            with self.subTest(quote=quote), self.assertRaises(ValueError):
                discussion_ai.parse_review({**MODEL_RESULT, "features": [quote]}, source)

    def test_reported_failure_is_kept_open_without_false_missing_features(self):
        review = discussion_ai.parse_review(MODEL_RESULT, app_request.field(app_request.parse_fields(BODY), "Requested features"))
        result = app_request.decide(1067, BODY, [ISSUE], {"patches": []}, REGISTRY, "MyGate", ai_review=review)
        self.assertEqual(["other bundle"], result["labels"])
        self.assertIn("requester reports that existing patches fail", result["comment"])
        self.assertIn("`Hide Ads`", result["comment"])
        self.assertNotIn("<img", result["comment"])
        self.assertNotIn("None of them has a patch", result["comment"])

    def test_review_reuses_valid_cached_result(self):
        response = io.BytesIO(json.dumps({"choices": [{"message": {"content": json.dumps(MODEL_RESULT)}}]}).encode())
        with patch.dict(os.environ, {"OPENROUTER_API_KEY": "test", "OPENROUTER_MODEL": "test/model"}), \
                patch("discussion_ai.urlopen", return_value=response) as request:
            result, marker = discussion_ai.review(BODY, ISSUE, ["Hide ads", "Unlock VIP"], "")
            cached, cached_marker = discussion_ai.review(BODY, ISSUE, ["Hide ads", "Unlock VIP"], marker)
        self.assertEqual(MODEL_RESULT, result)
        self.assertEqual(result, cached)
        self.assertEqual(marker, cached_marker)
        request.assert_called_once()
        payload = json.loads(request.call_args.args[0].data)
        material = json.loads(payload["messages"][1]["content"])
        self.assertEqual(["I tested the current patch and ads still appear. VIP is not unlocked."], material["comments"])
        self.assertNotIn("<img", material["features"])

    def test_failed_provider_is_skipped_for_the_rest_of_the_run(self):
        with tempfile.TemporaryDirectory() as directory:
            failure_file = os.path.join(directory, "failed")
            with patch.dict(os.environ, {"OPENROUTER_API_KEY": "test", "OPENROUTER_MODEL": "test/model",
                                      "DISCUSSION_AI_FAILURE_FILE": failure_file}), \
                    patch("discussion_ai.urlopen", side_effect=OSError) as request:
                self.assertEqual((None, ""), discussion_ai.review(BODY, ISSUE, ["Hide ads"], ""))
                self.assertEqual((None, ""), discussion_ai.review(BODY, ISSUE, ["Hide ads"], ""))
            self.assertTrue(os.path.exists(failure_file))
            request.assert_called_once()


if __name__ == "__main__":
    unittest.main()
