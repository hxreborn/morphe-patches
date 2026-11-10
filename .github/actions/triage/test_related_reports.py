#!/usr/bin/env python3

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from datetime import datetime, timedelta, timezone

from related_cases import PATCHES, SEEDS, bug_fields, cases, fields_body
from app_request import decide as request_verdict
from feature_request import decide as feature_verdict
from app_update import decide as update_verdict
from issue_comment import reported_app

HERE = Path(__file__).resolve().parent
TRIAGE = HERE / 'triage.py'
COMMENT = HERE.parents[1] / 'scripts' / 'issue_comment.py'


def evaluate(example, directory):
    kind = example['kind']
    if kind == 'identity':
        return {'verdict': reported_app(example['title'], example['body'], PATCHES)}
    if kind == 'update':
        body = fields_body({'App': 'Example Player', 'New version': example['version'],
                            'APK source': 'https://www.apkmirror.com/apk/example/player/',
                            'Patching this version': example['answer'],
                            'Error logs': example['logs']})
        trackers = ([{'number': 1, 'title': '[App Update]: Example Player 2.2', 'state': 'OPEN', 'labels': []}]
                    if example['duplicate'] else [])
        return update_verdict(1000, body, trackers, PATCHES)
    if kind == 'feature':
        return feature_verdict(fields_body({'App': example['app'], 'Feature description': example['description'],
                                            'Motivation': example['motivation']}), PATCHES)
    if kind == 'request':
        mode = example['mode']
        app = example.get('app', 'Example Player' if mode == 'supported' else 'New Journal')
        packages = {package['name']: package['packageName']
                    for patch in PATCHES['patches'] for package in patch['compatiblePackages']}
        package = packages[app] if mode == 'supported' else 'org.example.newapp'
        if mode == 'play_url':
            package = example.get('package', 'org.example.newapp')
            app = example.get('url', f'https://play.google.com/store/apps/details?id={package}')
        checked = ' ' if mode == 'unchecked' else 'x'
        features = {19: 'Add an AMOLED theme.',
                    54: 'Add an AMOLED theme that the other bundle does not provide.',
                    235: 'Unlock premium, remove advertising and add GmsCore support for the current app version.',
                    269: 'Remove advertising and add photo settings.'}[example['seed']]
        body = fields_body({'App': app, 'Package name': '_No response_' if mode == 'play_url' else package,
                            'Requested features': features,
                            'Acknowledgements': f'- [{checked}] I searched earlier requests.\n- [x] I have not requested it elsewhere.'})
        requests = [{'number': 1000, 'body': body, 'state': 'OPEN', 'author': {'login': 'requester'},
                     'comments': [], 'title': 'New app'}]
        if mode in {'open_duplicate', 'closed_duplicate'}:
            requests.append({**requests[0], 'number': 1, 'state': 'OPEN' if mode == 'open_duplicate' else 'CLOSED'})
        registry = {'bundles': []}
        if mode == 'other_bundle':
            registry['bundles'] = [{'repo': example.get('repo', 'example/other-patches'),
                                    'source': example.get('source', 'github'), 'name': 'Example bundle',
                                    'targetApps': [package]}]
        return request_verdict(1000, body, requests, PATCHES, registry)
    patches = directory / 'patches.json'
    patches.write_text(json.dumps(PATCHES))
    env = {**os.environ, 'ISSUE_TITLE': '', 'GITHUB_REPOSITORY': 'example/patches'}
    if kind == 'bug':
        fields = bug_fields(example['subject'])
        for key, value in example['changes'].items():
            if value is None:
                fields.pop(key, None)
            else:
                fields[key] = value
        body = directory / 'body'
        body.write_text(fields_body(fields))
        result = subprocess.run([sys.executable, str(TRIAGE), str(body), str(patches)],
                                capture_output=True, text=True, check=True, env=env)
        return json.loads(result.stdout)
    if kind == 'comment':
        closed = datetime(2026, 11, 1, tzinfo=timezone.utc)
        event = {'issue': {'number': 1, 'state': 'closed', 'created_at': '2026-10-01T00:00:00Z',
                           'closed_at': closed.isoformat(), 'labels': [{'name': example['label']}],
                           'title': 'Example Player', 'body': ''},
                 'comment': {'body': example['body'],
                             'created_at': (closed + timedelta(days=example['days'])).isoformat()}}
        event_path = directory / 'event.json'
        event_path.write_text(json.dumps(event))
        result = subprocess.run([sys.executable, str(COMMENT), '--unreleased' if example['unreleased'] else '--fixed',
                                 str(event_path), str(patches)], capture_output=True, text=True, env=env)
        if result.returncode == 3:
            return {'verdict': 'none'}
        result.check_returncode()
        marker = result.stdout.splitlines()[0]
        names = {'<!-- closed-issue-bot -->': 'closed', '<!-- unreleased-issue-bot -->': 'unreleased'}
        return {'verdict': names[marker]}
    raise ValueError(kind)


class RelatedReports(unittest.TestCase):
    def test_replace_release_emoji_with_release_notes_link(self):
        line = next(line.strip().rstrip(',') for line in (HERE.parents[2] / '.releaserc').read_text().splitlines()
                    if line.strip().startswith('"successComment":'))
        source = json.loads('{' + line + '}')['successComment']
        code = source[source.index('<%') + 2:source.index('%>')]
        script = "const body = new Function('issue', 'commits', 'nextRelease', 'branch', process.argv[1]);" \
                 "process.stdout.write(body({number:7,title:'Example',labels:[{name:'enhancement'}]}," \
                 "[],{version:'2.0.0'},{prerelease:false}));"
        result = subprocess.run(['node', '-e', script, code + '\nreturn body;'],
                                cwd=HERE.parents[2], capture_output=True, text=True, check=True,
                                env={**os.environ, 'GITHUB_REPOSITORY': 'example/patches'})
        self.assertIn('[releases page](https://github.com/example/patches/releases)', result.stdout)
        self.assertTrue(result.stdout.isascii())

    def test_check_five_cases_and_three_variants_per_seed(self):
        examples = list(cases())
        self.assertEqual(31, len(SEEDS))
        self.assertEqual(620, len(examples))
        with tempfile.TemporaryDirectory() as directory:
            for example in examples:
                with self.subTest(seed=example['seed'], branch=example['branch'], variant=example['variant']):
                    result = evaluate(example, Path(directory))
                    self.assertEqual(example.get('expected', 'pass'), result['verdict'])
                    if example['kind'] == 'request' and example['mode'] in {'supported', 'play_url'}:
                        packages = {package['name']: package['packageName']
                                    for patch in PATCHES['patches'] for package in patch['compatiblePackages']}
                        expected_package = (packages[example.get('app', 'Example Player')]
                                            if example['mode'] == 'supported' else example.get('package', 'org.example.newapp'))
                        self.assertEqual(expected_package, result['package'])
                    if 'labels' in example:
                        self.assertEqual(example['labels'], result.get('labels', []))


if __name__ == '__main__':
    unittest.main()
