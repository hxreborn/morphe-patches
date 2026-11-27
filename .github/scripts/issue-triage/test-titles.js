const assert = require('node:assert/strict');
const test = require('node:test');
const run = require('./triage');
const {loadApps, titleParts, validTitle} = require('./titles');

const apps = loadApps();
function issue(title, label, body) {
  return {number: 7, title, labels: [{name: label}], body, state: 'open', user: {login: 'reporter'}};
}

test('resolve app names and scoped titles from current and historical forms', () => {
  const cases = [
    ['[Feature]: Slow streaming', 'enhancement', '### App\nMoviebox', 'feat(MovieBox): Slow streaming'],
    ['bug(App): Premium playback stops', 'bug', '### App and version\nCastle for TV (another version, type it below)', 'bug(Castle for TV): Premium playback stops'],
    ['[Feature]: Add folders', 'feature', '### App\nProton Pass (F-Droid)', 'feat(Proton Pass (F-Droid)): Add folders'],
    ['[App Update]: MovieBox TV 1.2.1', 'app-update', '', 'update(MovieBox TV): Support 1.2.1'],
    ['update(MovieBox TV): Support 1.2.2', 'app-update', '### App\nMovieBox TV\n### New version\n1.2.1', 'update(MovieBox TV): Support 1.2.2'],
    ['feat(App): Mirror releases', 'feature', '### App\nAll apps', 'feat(Bundle): Mirror releases'],
    ['[App Update]:', 'app-update', '### App\n1Weather\n### APK source\nhttps://example.org/duolingo/\n### New version\n6.1', 'update(Duolingo): Support 6.1'],
  ];
  for (const [oldTitle, label, body, expected] of cases) {
    const report = issue(oldTitle, label, body);
    assert.equal(titleParts(report, run.parseForm(body), apps), expected);
    assert.ok(validTitle(expected));
    assert.equal(titleParts({...report, title: expected}, run.parseForm(body), apps), expected);
  }
  assert.ok(!validTitle('bug(App): Missing scope'));
  assert.ok(!validTitle('bug: Missing scope'));
  assert.equal(run.validateTitle('bug(MovieBox): Ask @maintainer', 'bug'), null);
});

test('format a flagged issue even when the model recommends closing it', async () => {
  const report = issue('[Feature]: video is stuck', 'feature', '### App\nMoviebox');
  report.labels.push({name: 'needs info'});
  const updates = [];
  const github = {rest: {issues: {
    get: async () => ({data: report}),
    update: async args => updates.push(args),
  }}};
  const context = {repo: {owner: 'example', repo: 'patches'}, eventName: 'issues', payload: {issue: report, action: 'opened'}};
  const core = {info() {}, warning() {}};
  const fetchImpl = async () => ({ok: true, text: async () => JSON.stringify({choices: [{message: {content: JSON.stringify({
    type: 'feature', close: true, title: 'feat(MovieBox): Video stalls during premium streaming',
  })}}]})});
  process.env.OPENROUTER_API_KEY = 'test';
  await run({github, context, core, fetchImpl, formatIssue: true});
  assert.deepEqual(updates, [{owner: 'example', repo: 'patches', issue_number: 7, title: 'feat(MovieBox): Video stalls during premium streaming'}]);
  delete process.env.OPENROUTER_API_KEY;
});

test('apply reviewed titles once and preserve concurrent edits', () => {
  const {applyPlan} = require('./backfill');
  const repo = 'example/patches';
  const plan = {repo, issues: [{number: 7, before: '[Bug]: Playback stops',
    after: 'bug(MovieBox): Playback stops', updatedAt: '2026-11-26T13:00:00Z'}]};
  const item = {title: plan.issues[0].before, updated_at: plan.issues[0].updatedAt};
  const writes = [];
  function request(command, endpoint, ...args) {
    assert.equal(command, 'api');
    assert.equal(endpoint, 'repos/example/patches/issues/7');
    if (!args.length) return JSON.stringify(item);
    assert.deepEqual(args, ['--method', 'PATCH', '-f', 'title=bug(MovieBox): Playback stops', '--silent']);
    item.title = args[3].slice(6);
    writes.push(item.title);
  }
  const apply = () => applyPlan(plan, repo, request, () => {});
  apply();
  apply();
  item.title = 'Newer title';
  assert.throws(apply, /changed since preview/);
  plan.issues.push({...plan.issues[0], number: 8, after: null});
  assert.throws(apply, /Review the title for #8/);
  assert.deepEqual(writes, ['bug(MovieBox): Playback stops']);
});
