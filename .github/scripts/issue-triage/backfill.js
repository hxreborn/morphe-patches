const {execFileSync} = require('node:child_process');
const fs = require('node:fs');
const {parseForm, askModels, parseModels, buildMessages, validateTitle} = require('./triage');
const {loadApps, titleParts, validTitle} = require('./titles');

function gh(...args) {
  return execFileSync('gh', args, {encoding: 'utf8', maxBuffer: 64 * 1024 * 1024}).trim();
}

function applyPlan(plan, repo, request, log) {
  if (plan.repo !== repo) throw new Error(`Plan is for ${plan.repo}, current repository is ${repo}`);
  for (const {number, after} of plan.issues) {
    if (!Number.isSafeInteger(number) || number < 1 || !validTitle(after)) throw new Error(`Review the title for #${number}`);
  }
  for (const {number, before, after, updatedAt} of plan.issues) {
    const endpoint = `repos/${repo}/issues/${number}`;
    const issue = JSON.parse(request('api', endpoint));
    if (issue.pull_request) throw new Error(`#${number} is a pull request`);
    if (issue.title === after) continue;
    if (issue.title !== before || issue.updated_at !== updatedAt) throw new Error(`#${number} changed since preview`);
    request('api', endpoint, '--method', 'PATCH', '-f', `title=${after}`, '--silent');
    log(`Renamed #${number}`);
  }
}

async function main() {
  const [mode, file] = process.argv.slice(2);
  if (mode && !(mode === '--ai' && !file) && !(mode === '--apply' && file)) {
    throw new Error('Usage: node backfill.js [--ai | --apply reviewed-plan.json]');
  }
  const repo = gh('repo', 'view', '--json', 'nameWithOwner', '--jq', '.nameWithOwner');
  if (mode === '--apply') {
    const plan = JSON.parse(fs.readFileSync(file, 'utf8'));
    applyPlan(plan, repo, gh, console.error);
    return;
  }

  const apiKey = process.env.OPENROUTER_API_KEY;
  if (mode === '--ai' && !apiKey) throw new Error('Set OPENROUTER_API_KEY for model-assisted titles');
  const apps = loadApps();
  const query = `query($owner:String!, $name:String!, $endCursor:String) {
    repository(owner:$owner, name:$name) {
      issues(first:100, after:$endCursor) {
        pageInfo {hasNextPage endCursor}
        nodes {number title body state updatedAt labels(first:100) {nodes {name}}}
      }
    }
  }`;
  const [owner, name] = repo.split('/');
  const pages = JSON.parse(gh('api', 'graphql', '--paginate', '--slurp', '-f', `query=${query}`,
    '-f', `owner=${owner}`, '-f', `name=${name}`));
  const issues = [];
  for (const page of pages) {
    for (const item of page.data.repository.issues.nodes) {
      const issue = {...item, labels: item.labels.nodes};
      const fields = parseForm(issue.body);
      let after = validTitle(issue.title) ? issue.title : titleParts(issue, fields, apps);
      if (mode === '--ai' && after !== issue.title && !issue.labels.some(label => label.name === 'app-update')) {
        const {decision} = await askModels({models: parseModels(process.env.OPENROUTER_MODEL), apiKey,
          messages: buildMessages(issue), core: {warning: console.error}});
        after = titleParts(issue, fields, apps, {...decision, title: validateTitle(decision.title, decision.type)}) || after;
      }
      issues.push({number: issue.number, state: issue.state, updatedAt: issue.updatedAt, before: issue.title, after});
    }
  }
  process.stdout.write(JSON.stringify({repo, issues}, null, 2) + '\n');
}

if (require.main === module) {
  main().catch(error => {console.error(error.message); process.exitCode = 1;});
}

module.exports = {applyPlan};
