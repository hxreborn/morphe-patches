from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'scripts'))

SEEDS = (
    (19, 'request', 'a dark theme for a new app'),
    (40, 'runtime', 'unreadable text in dark theme'),
    (43, 'patching', 'a missing asset in the TV edition'),
    (54, 'request', 'a theme absent from another bundle'),
    (62, 'runtime', 'a forced update prompt'),
    (76, 'identity', 'TV and mobile package confusion'),
    (86, 'feature', 'notification and navigation settings'),
    (92, 'followup', 'HD playback still failing', 'enhancement'),
    (101, 'patching', 'a localized Manager failure'),
    (106, 'install', 'a shared-user installation failure'),
    (149, 'followup', 'a startup crash after a fix', 'bug'),
    (196, 'followup', 'backup still failing', 'enhancement'),
    (235, 'request', 'a new version covered by another bundle'),
    (266, 'unreleased', 'a fix committed but not released'),
    (269, 'request', 'an app named by its Play Store URL'),
    (319, 'followup', 'a crash after launch', 'bug'),
    (373, 'version', 'a version targeted by a pre-release bundle'),
    (383, 'followup', 'verification still failing', 'bug'),
    (389, 'followup', 'unfinished theme changes', 'enhancement'),
    (456, 'runtime', 'a stream that will not load'),
    (485, 'followup', 'a sign-in regression during feature work', 'enhancement'),
    (611, 'runtime', 'a recurring session expiry prompt'),
    (620, 'supported_failure', 'backup failure on an already targeted version'),
    (653, 'runtime', 'a missing Android Auto entry'),
    (677, 'runtime', 'free chapters blocked by a premium patch'),
    (693, 'runtime', 'missing notifications'),
    (696, 'crash', 'a startup crash with an attached report'),
    (701, 'runtime', 'a session prompt despite the patch'),
    (702, 'runtime', 'excluded locations still used'),
    (706, 'runtime', 'reconnection after notification disconnect'),
    (709, 'runtime', 'watch synchronization failing'),
)

PATCHES = {'version': 'test', 'patches': [{'compatiblePackages': [
    {'name': 'Example Player', 'packageName': 'org.example.player', 'targets': [{'version': '1.0'}]},
    {'name': 'Example Player TV', 'packageName': 'org.example.player.tv', 'targets': [{'version': '1.0'}]},
    {'name': 'Example Mail', 'packageName': 'org.example.mail', 'targets': [{'version': '1.0'}]},
    {'name': 'Example Browser', 'packageName': 'org.example.browser', 'targets': [{'version': '1.0'}]},
]}]}


def fields_body(fields):
    return '\n\n'.join(f'### {key}\n\n{value}' for key, value in fields.items())


def bug_fields(subject):
    return {
        'App and version': 'Example Player 1.0',
        'What does not work': 'The patch has no visible effect',
        'Does the unpatched app do the same thing': 'The unpatched app works fine, only the patched one fails',
        'Device and Android version': 'Android 16',
        'APK source': 'https://www.apkmirror.com/apk/example/player/',
        'Steps to reproduce': f'Open the app and repeat the operation that causes {subject}.',
        'Expected behaviour': 'The selected operation completes with the selected patch enabled.',
        'Actual behaviour': f'The app reproduces {subject} on every attempt.',
        'Screenshots': 'https://github.com/user-attachments/assets/example',
        'Error logs': 'None',
    }


def branch(name, base, children):
    return name, [base, *children]


def bug_case(changes=None, verdict='pass', labels=()):
    return {'kind': 'bug', 'changes': changes or {}, 'expected': verdict, 'labels': list(labels)}


def comment_case(body, label='bug', days=1, unreleased=False, expected='none'):
    return {'kind': 'comment', 'body': body, 'label': label, 'days': days,
            'unreleased': unreleased, 'expected': expected}


def branches(seed):
    number, family, subject, *rest = seed
    if family == 'runtime':
        return [
            branch('optional log answers', bug_case(), [bug_case({'Error logs': value}) for value in ('.', '', '_No response_')]),
            branch('log explanations', bug_case({'Error logs': 'No crash occurs.'}), [
                bug_case({'Error logs': f'I want to use this feature, but the app reproduces {subject}.'}),
                bug_case({'Steps to reproduce': f'I want to use this feature, but the app reproduces {subject}.'}),
                bug_case({'Error logs': 'Log capture was not configured for this non-crashing problem.'})]),
            branch('patch has no effect on stock comparison', bug_case(), [
                bug_case({'Does the unpatched app do the same thing': value}) for value in (
                    'I have not tried the unpatched app', 'The unpatched app fails the same way', '')]),
            branch('unrelated required answers', bug_case(), [bug_case({key: '.'}, 'close') for key in (
                'Steps to reproduce', 'Actual behaviour', 'Screenshots')]),
            branch('regression with stock control', bug_case({'What does not work': 'Something else the app used to do stopped working'}), [
                bug_case({'What does not work': 'Something else the app used to do stopped working',
                          'Does the unpatched app do the same thing': value}, 'flag', ('needs info',)) for value in (
                    'I have not tried the unpatched app', 'The unpatched app fails the same way', 'I have not tried')]
            ),
        ]
    if family in {'patching', 'install', 'crash'}:
        happening = 'Patching failed' if family == 'patching' else 'Patching worked, install failed'
        common = ({'What does not work': 'The app will not open at all'} if family == 'crash'
                  else {'What does not work': None, 'What happened': happening})
        def case(log, verdict='pass', extra=None):
            return bug_case({**common, 'Error logs': log, **(extra or {})}, verdict,
                            ('needs info',) if verdict == 'flag' else ())
        return [
            branch('recognized error reports', case('Manager: 2.0\nPatchException: failed'), [
                case(value) for value in ('INSTALL_FAILED_SHARED_USER_INCOMPATIBLE',
                                         'Android: 16 (API 36)\nManager: 2.0',
                                         '```shell\njava.lang.IllegalStateException\n```')]),
            branch('missing error evidence', case('', 'flag'), [
                case(value, 'close' if value == 'None' else 'flag') for value in ('None', '_No response_', '```shell\n\n```')]),
            branch('attachments and paste links', case('https://github.com/user-attachments/files/1/trace.txt'), [
                case(value) for value in ('https://pastebin.com/trace', 'https://gist.github.com/example/trace',
                                         'https://rentry.co/trace')]),
            branch('source URLs are not reports', case('', 'flag'), [
                case('', 'flag', {'APK source': value}) for value in ('https://gist.github.com/example/trace',
                    'https://www.apkmirror.com/apk/example/player/', 'https://pastebin.com/trace')]),
            branch('log aliases', case('FATAL EXCEPTION: main'), [
                bug_case({**common, 'Error logs': None, key: 'FATAL EXCEPTION: main'}) for key in ('Debug log', 'Morphe logs', 'Logs')]),
        ]
    if family == 'followup':
        label = rest[0]
        return [
            branch('same problem after released fix', comment_case(f'The control for {subject} is missing after applying the released fix.', label), [
                comment_case(text, label) for text in (f'Please add the option back, {subject} is still a problem.',
                    f'The new setting for {subject} still does not work.', f'Please add a fix for {subject}, it still crashes.')]),
            branch('new version requests', comment_case('Please update patches to 2.0.', label, expected='closed'), [
                comment_case(text, label, expected='closed') for text in ('Can you update to 2.1?',
                    'Please support the latest app version.', 'Could you upgrade patches for 2.2?')]),
            branch('new feature requests', comment_case('Please add another feature, this is still missing.', label, expected='closed'), [
                comment_case(text, label, expected='closed') for text in ('Please add a new option, it is still missing.',
                    'Could you add another patch, this is still missing?', 'Please also add a new setting, still missing.')]),
            branch('follow-up time boundary', comment_case('It still crashes.', label, days=8), [
                comment_case('The new patch still crashes.', label, days=0),
                comment_case('It still crashes.', label, days=9, expected='closed'),
                comment_case('It still crashes.', label, days=-1, expected='closed')]),
            branch('confirmed fix and quoted complaints', comment_case('Thanks, it works now.', label), [
                comment_case(text, label) for text in ('> still broken\n\nThanks, it works now.',
                    'Fixed now, thank you.', '> it crashes\n\nIt works perfectly.')]),
        ]
    if family == 'unreleased':
        return [branch(name, comment_case(base, unreleased=True, expected=expected), [
            comment_case(text, unreleased=True, expected=expected) for text in children])
            for name, base, children, expected in (
                ('release timing', 'When will this be released?', ['Is the build available yet?', 'When is the fix coming?', 'Next release?'], 'unreleased'),
                ('status questions', 'Any update?', ['Any news?', 'Any progress?', 'Is anyone working on this?'], 'unreleased'),
                ('same problem before release', 'It still fails.', ['It still crashes.', 'The patch is broken.', 'It does not work.'], 'unreleased'),
                ('thanks before release', 'Thanks!', ['Thank you.', 'Great, thanks.', 'It works now, thanks.'], 'none'),
                ('quoted release questions', '> When will this be released?\nThanks!', ['> Any update?\nThanks.', '> still broken\nIt works now.', '> Next release?\nThank you.'], 'none'))]
    if family == 'identity':
        def identity(title, body, expected):
            return {'kind': 'identity', 'title': title, 'body': body, 'expected': expected}
        return [
            branch('TV title', identity('[Bug]: Example Player TV', '', 'Example Player TV'), [
                identity(title, '', 'Example Player TV') for title in ('Example Player TV crashes', 'EXAMPLE PLAYER TV', '[Feature]: Example Player TV settings')]),
            branch('mobile title', identity('Example Player', '', 'Example Player'), [
                identity(title, '', 'Example Player') for title in ('[Bug]: Example Player fails', 'EXAMPLE PLAYER', 'Example Player notifications')]),
            branch('package overrides shorter title', identity('Example Player', 'org.example.player.tv', 'Example Player TV'), [
                identity('Example Player', text, 'Example Player TV') for text in ('### Package name\n\norg.example.player.tv',
                    'https://play.google.com/store/apps/details?id=org.example.player.tv', 'Package: org.example.player.tv')]),
            branch('ambiguous packages use title', identity('Example Player TV', 'org.example.player org.example.player.tv', 'Example Player TV'), [
                identity(title, 'org.example.player org.example.player.tv', expected) for title, expected in (
                    ('Example Player', 'Example Player'), ('Example Mail', 'Example Mail'), ('Unlisted app', ''))]),
            branch('package boundaries', identity('Unlisted app', 'org.example.player.tv.extra', ''), [
                identity('Unlisted app', value, '') for value in ('org.example.player.extra', 'other.org.example.player.tv', 'org.example.player.tv_extra')]),
        ]
    if family == 'feature':
        def feature(app='Example Player', description=None, motivation='This makes the existing workflow easier to use.', expected='pass'):
            return {'kind': 'feature', 'app': app, 'description': description or f'Add support for {subject}.',
                    'motivation': motivation, 'expected': expected}
        return [
            branch('supported apps', feature(), [feature(app=value) for value in ('Example Player TV', 'Example Mail', 'Example Player 1.0')]),
            branch('package and normalized names', feature(app='org.example.player'), [
                feature(app=value) for value in ('EXAMPLE PLAYER', 'ExamplePlayer', 'org.example.player.tv')]),
            branch('missing optional app metadata', feature(app=''), [feature(app=value) for value in ('_No response_', 'all', '1.0')]),
            branch('placeholder motivation', feature(motivation='.', expected='close'), [
                feature(motivation=value, expected='close') for value in ('N/A', 'None', '0')]),
            branch('new unsupported apps use app requests', feature(app='Unlisted Journal', expected='close'), [
                feature(app=value, expected='close') for value in ('Unlisted Calendar', 'Unknown Notes', 'Different Music')]),
        ]
    if family == 'supported_failure':
        def update(answer, version='1.0', expected='flag', duplicate=False, logs='Backup does not restore saved data on the patched app.'):
            return {'kind': 'update', 'answer': answer, 'version': version, 'expected': expected,
                    'duplicate': duplicate, 'labels': [], 'logs': logs}
        return [
            branch('runtime failure on supported version', update('Patching works, the app misbehaves'), [
                update('PATCHING WORKS, THE APP MISBEHAVES'),
                update('Patching works, the app misbehaves', logs='```shell\nBackup does not restore data.\n```'),
                update('Patching works, the app misbehaves', logs='https://github.com/user-attachments/files/1/trace.txt')]),
            branch('patching failure on supported version', update('Tried but patching failed'), [
                update('Tried but patching failed', version=version) for version in ('v1.0', '1.0 (100)', '1.0.0')]),
            branch('ordinary already supported update', update('Not tried', expected='close'), [
                update(answer, expected='close') for answer in ('Patching works', '', '_No response_')]),
            branch('failure on new untargeted version', update('Tried but patching failed', version='2.0', expected='pass'), [
                update('Tried but patching failed', version=version, expected='pass') for version in ('2.1', '3.0', '4.0')]),
            branch('duplicate update despite unsupported failure', update('Tried but patching failed', version='2.0', expected='close', duplicate=True), [
                update('Tried but patching failed', version=version, expected='close', duplicate=True) for version in ('2.0.1', '2.1', '2.2')]),
        ]
    if family == 'version':
        return [
            branch('targeted versions', bug_case({'Error logs': 'Manager: 2.0'}), [
                bug_case({'App and version': value, 'Error logs': 'Manager: 2.0'}) for value in (
                    'Example Player v1.0', 'Example Player 1.0 build42', 'Example Player V1.0')]),
            branch('newer untargeted versions', bug_case({'App and version': 'Example Player 2.0'}, 'flag', ('untargeted version',)), [
                bug_case({'App and version': f'Example Player {version}'}, 'flag', ('untargeted version',)) for version in ('2.1', '3.0', '4.0')]),
            branch('older untargeted versions', bug_case({'App and version': 'Example Player 0.9'}, 'flag', ('untargeted version',)), [
                bug_case({'App and version': f'Example Player {version}'}, 'flag', ('untargeted version',)) for version in ('0.8', '0.7', '0.6')]),
            branch('another version needs an answer', bug_case({'App and version': 'Example Player (another version, type it below)'}, 'flag', ('needs info',)), [
                bug_case({'App and version': 'Example Player (another version, type it below)', 'Other version': value}, 'flag', ('needs info',)) for value in ('', '_No response_', None)]),
            branch('another version supplied', bug_case({'App and version': 'Example Player (another version, type it below)', 'Other version': '1.0'}), [
                bug_case({'App and version': 'Example Player (another version, type it below)', 'Other version': value}) for value in ('v1.0', 'V1.0', '1.0 build42')]),
        ]
    if family == 'request':
        def request(mode='new', **changes):
            return {'kind': 'request', 'mode': mode, **changes}
        if number in {54, 235}:
            external = branch('other bundle coverage', request('other_bundle', expected='flag', labels=['needs info', 'other bundle']), [
                request('other_bundle', repo=repo, source=source, expected='flag', labels=['needs info', 'other bundle'])
                for repo, source in (('example/second-patches', 'github'), ('example/third-patches', 'gitlab'), ('another/patches', 'github'))])
        else:
            external = branch('open duplicate', request('open_duplicate', expected='close'), [request('open_duplicate', app=value, expected='close') for value in ('New Notes', 'New Calendar', 'New Tasks')])
        if number == 269:
            new_app = branch('package from Play Store URL', request('play_url'), [
                request('play_url', url=url) for url in (
                    'https://play.google.com/store/apps/details?hl=en&id=org.example.newapp',
                    '[New Journal](https://play.google.com/store/apps/details?id=org.example.newapp)',
                    'New Journal, https://play.google.com/store/apps/details?id=org.example.newapp')])
        else:
            new_app = branch('new app form', request(), [request(app=value) for value in ('New Notes', 'New Calendar', 'New Tasks')])
        return [
            new_app,
            branch('supported package', request('supported', expected='close'), [request('supported', app=value, expected='close') for value in ('Example Player TV', 'Example Mail', 'Example Browser')]),
            external,
            branch('closed earlier request', request('closed_duplicate', expected='flag', labels=['duplicate']), [request('closed_duplicate', app=value, expected='flag', labels=['duplicate']) for value in ('New Notes', 'New Calendar', 'New Tasks')]),
            branch('unchecked acknowledgements', request('unchecked', expected='close'), [request('unchecked', app=value, expected='close') for value in ('New Notes', 'New Calendar', 'New Tasks')]),
        ]
    raise ValueError(family)


def cases():
    for seed in SEEDS:
        groups = branches(seed)
        if len(groups) != 5 or any(len(group[1]) != 4 for group in groups):
            raise ValueError(seed)
        for name, examples in groups:
            for variant, example in enumerate(examples):
                yield {'seed': seed[0], 'family': seed[1], 'subject': seed[2],
                       'branch': name, 'variant': variant, **example}
