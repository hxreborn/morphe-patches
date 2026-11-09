const assert = require('assert');
const fs = require('fs');
const vm = require('vm');
const test = require('node:test');

const source = fs.readFileSync(__dirname + '/../../main/java/app/hxreborn/extension/terabox/GoogleLoginChromeClient.java', 'utf8');
const script = source.split('private static final String LOGIN_PAGE_SCRIPT = """')[1]
  .split('""";')[0].replace(/\\\n\s*/g, '').trim();

function loginPage() {
  let poll;
  function Element() {}
  Element.prototype.setAttribute = function() {};
  function HTMLScriptElement() {}
  Object.defineProperty(HTMLScriptElement.prototype, 'src', {
    configurable: true,
    get() { return ''; },
    set(value) {},
  });
  const page = {
    Element,
    HTMLScriptElement,
    XMLHttpRequest: function() {},
    document: { querySelector() { return null; } },
    setInterval(callback) { poll = callback; return 1; },
    clearInterval() {},
    setTimeout() {},
    initialize(config) { this.config = config; return 'initialized'; },
  };
  page.XMLHttpRequest.prototype.open = function() {};
  page.window = page;
  vm.createContext(page);
  return {
    page,
    run(code) { return vm.runInContext(code, page); },
    inject() { vm.runInContext(script, page); },
    poll() { poll(); },
  };
}

for (const assignment of [
  'google = {accounts: {id: {initialize}}}',
  'google = {}; google.accounts = {}; google.accounts.id = {}; google.accounts.id.initialize = initialize',
]) {
  test('captures initialization before polling with ' + assignment, () => {
    const login = loginPage();
    login.inject();
    login.run(assignment);
    login.run('google.accounts.id.initialize({callback() { window.delivered = true; }})');
    login.poll();
    login.run('__tbDeliver("credential")');
    assert.strictEqual(login.page.delivered, true);
  });
}

test('captures initialization when Google is already loaded', () => {
  const login = loginPage();
  login.run('google = {accounts: {id: {initialize}}}');
  login.inject();
  login.run('google.accounts.id.initialize({callback() { window.delivered = true; }})');
  login.run('__tbDeliver("credential")');
  assert.strictEqual(login.page.delivered, true);
});

test('keeps initialization options and return value', () => {
  const login = loginPage();
  login.inject();
  login.run('google = {accounts: {id: {initialize}}}');
  const returned = login.run('google.accounts.id.initialize({client_id: "client", context: "signin", callback() {}})');
  assert.strictEqual(returned, 'initialized');
  assert.strictEqual(login.page.google.accounts.id.config.client_id, 'client');
  assert.strictEqual(login.page.google.accounts.id.config.context, 'signin');
});

test('delivers each credential once after repeated script injection', () => {
  const login = loginPage();
  login.inject();
  login.run('google = {accounts: {id: {initialize}}}');
  login.inject();
  login.poll();
  login.run('window.deliveries = 0; google.accounts.id.initialize({callback() { window.deliveries++; }})');
  login.run('__tbDeliver("first"); __tbDeliver("first"); __tbDeliver("second")');
  assert.strictEqual(login.page.deliveries, 2);
});

test('passes initialization without a callback to Google', () => {
  const login = loginPage();
  login.inject();
  login.run('google = {accounts: {id: {initialize}}}');
  const returned = login.run('google.accounts.id.initialize(null)');
  assert.strictEqual(returned, 'initialized');
  assert.strictEqual(login.page.google.accounts.id.config, null);
});
