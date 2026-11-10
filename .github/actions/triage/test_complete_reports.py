#!/usr/bin/env python3

import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name('triage.py')
APPS = {'version': 'test', 'patches': [{'compatiblePackages': [
    {'name': 'Example', 'packageName': 'com.example.app', 'targets': [{'version': '1.0'}]},
]}]}


def report(failure='The patch has no visible effect', logs='None', screenshots='https://example.com/screen.png',
           steps='Open the app and select the location to exclude.',
           expected='The selected location is excluded.', actual='The selected location is still used.'):
    fields = {
        'App and version': 'Example 1.0',
        'What does not work': failure,
        'Does the unpatched app do the same thing': 'The unpatched app works fine, only the patched one fails',
        'Device and Android version': 'Android 16',
        'APK source': 'https://example.com/app.apk',
        'Steps to reproduce': steps,
        'Expected behaviour': expected,
        'Actual behaviour': actual,
        'Screenshots': screenshots,
        'Error logs': logs,
    }
    return '\n\n'.join(f'### {key}\n\n{value}' for key, value in fields.items())


def verdict(body):
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        (root / 'body').write_text(body)
        (root / 'apps.json').write_text(json.dumps(APPS))
        result = subprocess.run(['python3', str(SCRIPT), str(root / 'body'), str(root / 'apps.json')],
                                env={**os.environ, 'ISSUE_TITLE': '', 'GITHUB_REPOSITORY': 'example/patches'},
                                capture_output=True, text=True, check=True)
        return json.loads(result.stdout)


class CompleteReports(unittest.TestCase):
    def test_accept_non_crashing_reports_without_logs(self):
        for failure in ('The patch has no visible effect', 'The patched feature is still locked or still shows ads',
                        'A patch is missing from the list in Manager',
                        'Something else the app used to do stopped working'):
            for logs in ('None', '.', '', 'Error', 'No crash. Location exclusion does not work.',
                         'LogFox recording requires access I have not configured.'):
                with self.subTest(failure=failure, logs=logs):
                    self.assertEqual('pass', verdict(report(failure=failure, logs=logs))['verdict'])

    def test_keep_crash_log_requirement(self):
        result = verdict(report(failure='The app opens, then crashes', logs=''))
        self.assertEqual('flag', result['verdict'])
        self.assertEqual(['needs info'], result['labels'])
        self.assertEqual('close', verdict(report(failure='The app opens, then crashes', logs='.'))['verdict'])

    def test_apk_source_link_does_not_count_as_log(self):
        body = report(failure='The app opens, then crashes', logs='').replace(
            'https://example.com/app.apk', 'https://www.apkmirror.com/apk/example/app/')
        result = verdict(body)
        self.assertEqual('flag', result['verdict'])
        self.assertIn('The Error logs field has no log', result['comment'])

    def test_only_error_logs_field_supplies_log_signal(self):
        body = report(failure='The app opens, then crashes', logs='').replace(
            'https://example.com/app.apk', 'https://gist.github.com/example/abcd')
        self.assertEqual('flag', verdict(body)['verdict'])
        self.assertEqual('pass', verdict(report(failure='The app opens, then crashes',
                                               logs='https://gist.github.com/example/abcd'))['verdict'])

    def test_reject_unrecognized_log_host(self):
        result = verdict(report(failure='The app opens, then crashes', logs='https://example.com/log.txt'))
        self.assertEqual('flag', result['verdict'])
        self.assertIn('The Error logs field has no log', result['comment'])

    def test_keep_checks_for_other_required_answers(self):
        for field in ('steps', 'expected', 'actual', 'screenshots'):
            with self.subTest(field=field):
                self.assertEqual('close', verdict(report(**{field: '.'}))['verdict'])

    def test_keep_log_validation_for_unknown_failure_choices(self):
        for failure in ('', 'The app crashes on startup', 'Other', '_No response_'):
            with self.subTest(failure=failure):
                self.assertEqual('close', verdict(report(failure=failure, logs='None'))['verdict'])

    def test_patching_failure_takes_precedence_over_runtime_choice(self):
        for happened in ('Patching failed', 'Patching started and stopped with an error'):
            with self.subTest(happened=happened):
                body = report(logs='None') + f'\n\n### What happened\n\n{happened}'
                self.assertEqual('close', verdict(body)['verdict'])
                self.assertEqual('flag', verdict(body.replace('### Error logs\n\nNone',
                                                             '### Error logs\n\n'))['verdict'])

    def test_accept_optional_logs_in_code_fences_and_normalized_headings(self):
        for logs in ('```shell\nNone\n```', '~~~text\nNo crash occurs.\n~~~', '_No response_'):
            with self.subTest(logs=logs):
                body = report(logs=logs).replace('### What does not work', '### WHAT DOES NOT WORK')
                self.assertEqual('pass', verdict(body)['verdict'])
        body = report().replace('### Error logs\n\nNone', '')
        self.assertEqual('pass', verdict(body)['verdict'])

    def test_log_signal_does_not_leak_from_other_fields(self):
        for field, value in (('source', 'https://gist.github.com/example/log'),
                             ('screenshots', 'https://github.com/user-attachments/files/123/log.txt'),
                             ('steps', 'Open the app. FATAL EXCEPTION: main is shown on screen.')):
            with self.subTest(field=field):
                options = {'failure': 'The app opens, then crashes', 'logs': ''}
                if field != 'source':
                    options[field] = value
                body = report(**options)
                if field == 'source':
                    body = body.replace('https://example.com/app.apk', value)
                result = verdict(body)
                self.assertEqual('flag', result['verdict'])
                self.assertIn('The Error logs field has no log', result['comment'])

    def test_keep_supported_log_links_in_log_field_aliases(self):
        for heading in ('Error logs', 'Debug log', 'Morphe logs', 'Logs'):
            for link in ('https://github.com/user-attachments/files/123/trace.txt',
                         'https://pastebin.com/abcd', 'https://GIST.GITHUB.COM/example/abcd'):
                with self.subTest(heading=heading, link=link):
                    body = report(failure='The app opens, then crashes', logs=link)
                    body = body.replace('### Error logs', f'### {heading}')
                    self.assertEqual('pass', verdict(body)['verdict'])

    def test_paste_host_lookalikes_do_not_count_as_logs(self):
        for link in ('https://pastebin.com.example.org/trace', 'https://gist.github.com.example.org/trace',
                     'https://pastebin.com@example.org/trace'):
            with self.subTest(link=link):
                result = verdict(report(failure='The app opens, then crashes', logs=link))
                self.assertEqual('flag', result['verdict'])
                self.assertIn('The Error logs field has no log', result['comment'])

    def test_existing_feature_intent_does_not_close_documented_failure(self):
        intent = 'I want to exclude a location, but the existing exclusion button does nothing.'
        for field in ('steps', 'logs'):
            with self.subTest(field=field):
                self.assertEqual('pass', verdict(report(**{field: intent}))['verdict'])

    def test_keep_feature_only_report_close(self):
        body = '### App and version\n\nExample 1.0\n\n### Bug description\n\nPlease add an option for custom themes.'
        self.assertEqual('close', verdict(body)['verdict'])
        self.assertEqual('close', verdict(report(steps='Please add an option for custom themes.'))['verdict'])


if __name__ == '__main__':
    unittest.main()
