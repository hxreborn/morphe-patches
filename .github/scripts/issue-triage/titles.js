const fs = require('node:fs');
const path = require('node:path');

const PREFIX = /^(bug|feat|update|question)\((.+?)\):\s*(.*)$/i;
const LEGACY = /^\s*\[(?:bug|not working|broken feature|feature(?: request)?|enhancement|app (?:request|update)|アプリの更新)\]\s*:?\s*|^(?:bug|feat|question):\s*/i;
const UNSAFE = /[@<>\x00-\x1f\x7f\u202a-\u202e\u2066-\u2069]|#\d|https?:|www\./i;
const norm = (text) => text.normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
const escapePattern = (text) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

function loadApps() {
  const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, '../../../patches-list.json'), 'utf8'));
  const apps = new Map();
  const factors = new Map();
  for (const patch of manifest.patches) {
    for (const pkg of patch.compatiblePackages || []) {
      apps.set(pkg.name, pkg.name);
      apps.set(pkg.packageName, pkg.name);
      const variants = factors.get(pkg.packageName) || new Set();
      variants.add(pkg.formFactor || 'MOBILE');
      factors.set(pkg.packageName, variants);
    }
  }
  for (const [pkg, variants] of factors) {
    if (variants.size < 2) continue;
    for (const variant of variants) {
      const label = { MOBILE: 'Mobile', WEARABLE: 'Wear OS' }[variant] || variant[0] + variant.slice(1).toLowerCase();
      const name = `${apps.get(pkg)} (${label})`;
      apps.set(name, name);
    }
  }
  return [...apps].sort(([a], [b]) => b.length - a.length);
}

function namedApps(text, apps) {
  const matches = [];
  let remaining = text;
  for (const [alias, name] of apps) {
    const pattern = new RegExp(`(?<![\\p{L}\\p{N}])${escapePattern(alias)}(?![\\p{L}\\p{N}])`, 'giu');
    if (pattern.test(remaining)) {
      matches.push(name);
      remaining = remaining.replace(pattern, ' ');
    }
  }
  return [...new Set(matches)];
}

function safeApp(value) {
  return value && value.length <= 85 && !UNSAFE.test(value) && !/[\[\]`]|\):/.test(value) &&
    !/^(?:app|app name|unknown|none|n\/a|_no response_)$/i.test(value) && /\p{L}/u.test(value);
}

function reportType(labels, title, fields, decision) {
  if (labels.has('app-update')) return 'update';
  if (labels.has('bug')) return 'bug';
  if (['feature', 'enhancement', 'app request'].some(label => labels.has(label))) return 'feat';
  if (fields['New version']) return 'update';
  if (fields['Bug description'] || fields['Actual behaviour']) return 'bug';
  if (fields['Feature description'] || fields['Requested features']) return 'feat';
  const prefix = PREFIX.exec(title)?.[1] || /^(bug|question|feat):/i.exec(title)?.[1];
  if (prefix) return prefix.toLowerCase();
  if (/^\[app update/i.test(title)) return 'update';
  if (/^\[(?:feature|enhancement|app request)/i.test(title)) return 'feat';
  if (/^\[(?:bug|not working|broken feature)/i.test(title)) return 'bug';
  return {bug: 'bug', feature: 'feat', question: 'question'}[decision?.type];
}

function reportedApp(issue, fields, apps, oldSummary, current, proposed, type) {
  const raw = (fields['App and version'] || fields.App || '').trim();
  let typed = raw.replace(/[,\s–—-]*https?:\/\/.*$/is, '').trim().replace(/^\[([^\]]+)\]\($/, '$1');
  if (fields['App and version']) typed = typed.replace(/\s+(?:\(another version.*|[vp]?\d+(?:\.\d+)+.*)$/i, '');
  if (/^(?:all(?: apps)?|not applicable|universal patches|bundle)$/i.test(typed)) return 'Bundle';
  const selected = apps.find(([alias]) => typed && norm(alias) === norm(typed));
  const titleApps = namedApps(oldSummary, apps);
  const sourceApps = namedApps(fields['APK source'] || '', apps);
  if (selected && sourceApps.some(name => selected[1].startsWith(name + ' ('))) return selected[1];
  if (sourceApps.length === 1 && (!titleApps.length || titleApps.includes(sourceApps[0])) &&
      (fields['App and version'] || type === 'update')) return sourceApps[0];
  if (selected) return selected[1];
  const fieldApps = namedApps(typed, apps);
  if (fieldApps.length) return fieldApps.join(' & ');
  if (safeApp(typed)) return typed;
  const packageApps = namedApps(fields['Package name'] || raw, apps);
  if (packageApps.length === 1) return packageApps[0];
  if (current && safeApp(current[2])) return current[2];
  const tracker = type === 'update' && /^(.+?)\s+((?:Android\s+)?[a-z]*\d+(?:\.\d+)+[\w.+() -]*)$/i.exec(oldSummary);
  if (tracker && safeApp(tracker[1])) return tracker[1];
  if (titleApps.length) return titleApps.join(' & ');
  const evidence = issue.title + ' ' + (issue.body || '');
  if (proposed && safeApp(proposed[2]) && norm(evidence).includes(norm(proposed[2]))) return proposed[2];
  if (issue.labels.some(label => label.name === 'app request') && safeApp(oldSummary)) return oldSummary;
  const names = namedApps(fields['Package name'] || fields['Bug description'] || '', apps);
  return names.length === 1 ? names[0] : null;
}

function titleParts(issue, fields, apps, decision) {
  const title = issue.title.trim();
  const current = PREFIX.exec(title);
  const proposed = PREFIX.exec(decision?.title?.trim() || '');
  const labels = new Set(issue.labels.map(label => label.name));
  const oldSummary = (current?.[3] ?? title.replace(/^\u200b+/, '').replace(LEGACY, '')).trim();
  const type = reportType(labels, title, fields, decision);
  const app = reportedApp(issue, fields, apps, oldSummary, current, proposed, type);
  if (!safeApp(app) || !type) return null;
  let summary = oldSummary;
  if (type === 'update') {
    const tracked = current && /^Support (.+)$/i.exec(current[3]);
    const legacy = new RegExp(`^${escapePattern(app)}\\s+(.+)$`, 'i').exec(oldSummary);
    const version = tracked?.[1] || legacy?.[1] || fields['New version'];
    if (version && /\d/.test(version) && /^[\w.+() -]{1,90}$/.test(version)) summary = `Support ${version}`;
  } else if (proposed && norm(proposed[2]) === norm(app) && proposed[3] && !UNSAFE.test(proposed[3]) && proposed[3].length <= 110) {
    summary = proposed[3];
  }
  if (labels.has('app request') && !current && !proposed && fields['Requested features']) {
    const requests = fields['Requested features'].split(/, |\r?\n/).map(s => s.replace(/^[-*]\s*/, '').trim())
      .filter(s => s && !/something else|described below/i.test(s));
    if (requests.length) summary = requests.join(', ');
  }
  if (summary === oldSummary && !current) {
    const escaped = escapePattern(app);
    summary = summary.replace(new RegExp(`^(?:${escaped})(?:\\s*[:-]\\s*|\\s+)|\\s+(?:for|on|in)\\s+${escaped}$`, 'gi'), '').trim();
  }
  if (!summary || norm(summary) === norm(app)) {
    summary = (fields['Requested features'] || fields['Feature description'] || fields['Actual behaviour'] ||
      fields['Bug description'] || fields['What does not work'] || '').split(/\r?\n/)[0].trim();
  }
  summary = summary.replace(/^[-*]\s+/, '').trim();
  if (!summary || UNSAFE.test(summary) || summary.length > 200) return null;
  summary = summary[0].toUpperCase() + summary.slice(1);
  const result = `${type}(${app}): ${summary}`;
  return result.length <= 256 ? result : null;
}

function validTitle(title) {
  if (typeof title !== 'string' || title.length > 256 || UNSAFE.test(title)) return false;
  const match = PREFIX.exec(title);
  return Boolean(match && safeApp(match[2]) && match[3].trim() && title === `${match[1].toLowerCase()}(${match[2]}): ${match[3].trim()}`);
}

module.exports = {loadApps, titleParts, validTitle};
