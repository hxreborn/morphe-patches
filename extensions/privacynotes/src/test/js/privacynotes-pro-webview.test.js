const fs = require('fs');
const assert = require('assert');
const js = fs.readFileSync(__dirname + '/../../main/js/privacynotes-pro-webview.js', 'utf8');

const store = {};
global.localStorage = {
  getItem: key => (key in store ? store[key] : null),
  setItem: (key, value) => { store[key] = String(value); },
  removeItem: key => { delete store[key]; },
};
global.location = { href: 'https://tauri.localhost/' };

let reply;
global.window = { fetch: async () => reply };
eval(js);
const wrapped = window.fetch;
eval(js);
assert.strictEqual(window.fetch, wrapped, 'wrapper installed once');

const json = (status, body) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const call = async (path, response) => {
  reply = response;
  const result = await window.fetch('https://sync.privacynotes.app' + path);
  return { status: result.status, body: await result.json() };
};

(async () => {
  let result = await call('/functions/v1/register-device', json(200, { is_pro: false, device_id: 'd' }));
  assert.deepStrictEqual(result.body, { is_pro: true, device_id: 'd' }, 'free registration reports Pro');

  result = await call('/rest/v1/rpc/is_pro', json(200, false));
  assert.strictEqual(result.body, true, 'is_pro answers true');

  result = await call('/rest/v1/rpc/device_heartbeat', json(200, false));
  assert.strictEqual(result.body, false, 'heartbeat untouched while the device is registered');

  result = await call('/functions/v1/register-device', json(403, { error: 'device_limit_reached', limit: 2 }));
  assert.deepStrictEqual([result.status, result.body], [200, { is_pro: true }], 'device cap refusal becomes a registration');

  result = await call('/rest/v1/rpc/device_heartbeat', json(200, false));
  assert.strictEqual(result.body, true, 'heartbeat answers true for a device the server never registered');

  result = await call('/functions/v1/register-device', json(200, { is_pro: false }));
  result = await call('/rest/v1/rpc/device_heartbeat', json(200, false));
  assert.strictEqual(result.body, false, 'a later real registration restores the server verdict');

  result = await call('/functions/v1/register-device', json(401, { error: 'mfa_required' }));
  assert.deepStrictEqual([result.status, result.body], [401, { error: 'mfa_required' }], 'other refusals pass through');

  result = await call('/rest/v1/notes', json(200, [{ id: 1 }]));
  assert.deepStrictEqual(result.body, [{ id: 1 }], 'unrelated requests pass through');
  console.log('PASS');
})().catch(error => { console.error(error); process.exit(1); });
