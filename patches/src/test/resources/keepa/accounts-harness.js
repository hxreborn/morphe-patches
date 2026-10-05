const assert = require("node:assert/strict");
const fs = require("node:fs");

const runtime = fs.readFileSync(process.argv[2], "utf8");
const token = c => c.repeat(64);
const store = {
  accounts: { primaryId: "a", allocation: "auto", accounts: [
    { id: "a", username: "alice", token: token("a"), state: "ok", limit: 200, tracked: 1 },
    { id: "b", username: "bob", token: token("b"), state: "ok", limit: 200, tracked: 1 },
    { id: "c", username: "carol", token: token("c"), state: "invalid", limit: 200, tracked: 0 },
  ] },
  owners: { X1: ["b"], X2: ["a"] },
  pending: "",
};
const bridgeCalls = [];
const allocations = [];
let bridgeFailure = null;
let snapshot = () => ({ ...store, pending: store.pending ? JSON.parse(store.pending) : null, summary: "3 accounts", limitTotal: 600 });
const bridge = (method, json) => {
  const args = json ? JSON.parse(json) : null;
  bridgeCalls.push([method, args]);
  if (bridgeFailure) return JSON.stringify({ error: bridgeFailure });
  if (method === "allocate") return JSON.stringify({ value: allocations.shift() || "" });
  if (method === "mergeOverviews") return JSON.stringify({ value: { merged: args.responses, commit: args.commit } });
  if (method === "applyReject") store.accounts.accounts.find(a => a.id === args.accountId).state = args.status === 401 ? "invalid" : "throttled";
  if (method === "setPrimary") store.accounts.primaryId = args.id;
  if (method === "remove") {
    store.accounts.accounts = store.accounts.accounts.filter(a => a.id !== args.id);
    if (store.accounts.primaryId === args.id) store.accounts.primaryId = store.accounts.accounts.length ? store.accounts.accounts[0].id : "";
  }
  if (method === "onServerData") {
    const existing = store.accounts.accounts.find(a => a.username === args.username);
    const account = existing || { id: args.username[0], username: args.username, limit: 200, tracked: 0 };
    if (!existing) store.accounts.accounts.push(account);
    Object.assign(account, { token: args.token, state: "ok" });
    store.accounts.primaryId = account.id;
  }
  if (method === "setPending") store.pending = json;
  if (method === "clearPending") store.pending = "";
  return JSON.stringify({ value: snapshot() });
};

class WebSocketMessage { constructor(path, type) { Object.assign(this, { path, type, id: 1 }); } }
class WebSocketRejectReason { constructor(data) { this.data = data; } }
class SettingsService { load() { return Promise.resolve(); } }
class PushService { proactivelyCheckAndRegisterIfEnabled() { return Promise.resolve(); } }
class EnvironmentInjector {}
const services = new Map([[SettingsService, new SettingsService()], [PushService, new PushService()]]);
const injector = { get: t => services.get(t), runInContext: f => f() };
const modules = {
  "./src/app/services/network/web-socket-message.ts": { t: WebSocketMessage },
  "./src/app/services/network/web-socket-reject-reason.ts": { V: WebSocketRejectReason },
  "./src/app/services/settings/settings.service.ts": { h: SettingsService },
  "./src/app/services/push/push.service.ts": { U: PushService },
  "./node_modules/@nativescript/core/utils/native-helper.android.js": { yA: { getApplicationContext: () => "context" } },
};
const hxRequire = path => modules[path];
const java = { lang: { String: {}, Class: { forName: () => ({ getMethod: () => ({ invoke: (_, [, method, json]) => bridge(method, json) }) }) } } };
const android = { content: { Context: {} } };
const core = { WQX: t => (t === EnvironmentInjector ? injector : null), uvJ: EnvironmentInjector };
const replies = new Map();
const navigation = { url: "/manage", events: [] };
const sockets = [];

const { hx, NetworkService } = new Function("hxRequire", "e", "java", "android", "replies", "navigation", "sockets", `${runtime}
class NetworkService {
  constructor() {
    this.deferredStorage = new Map();
    this.awaitingTurnstile = false;
    this.destroyed = false;
    this.storageService = { settings: { token: "${token("a")}", accountType: 2, username: "alice", pushToken: "fcm" },
      getSettings() { return { ...this.settings }; }, setSettings(s) { this.settings = s; return true; }, getDefaultSettings: () => ({}) };
    this.navigationService = { getCurrentUrl: () => navigation.url, goToLogin() { navigation.url = "/login"; },
      _router: { events: { subscribe: h => navigation.events.push(h) } }, goToHomeTab() { navigation.url = "/home"; } };
    sockets.push(this);
    hxAccountsRuntime.attach(this, e.WQX, e);
    this.reconnect(true);
  }
  reconnect() { this.connectedAs = this.storageService.getSettings().token[0]; }
  ngOnDestroy() { this.destroyed = true; }
  flushAllRequests() { this.flushed = true; }
  handleTurnstileTokenObtained() {}
  handleTurnstileVerificationFailed() {}
  request(m) {
    const r = hxAccountsRuntime.route(this, m);
    if (r) return r;
    const reply = replies.get(this.connectedAs + ":" + m.path + ":" + m.type);
    return reply ? reply(m) : Promise.reject(new (hxRequire("./src/app/services/network/web-socket-reject-reason.ts").V)({ status: 400 }));
  }
}
return { hx: hxAccountsRuntime, NetworkService };`)(hxRequire, core, java, android, replies, navigation, sockets);

const primary = new NetworkService();
const message = (path, type, fields) => Object.assign(new WebSocketMessage(path, type), fields);
const calls = method => bridgeCalls.filter(([m]) => m === method).map(([, json]) => json);
const flush = () => new Promise(resolve => setImmediate(resolve));

(async () => {
  await flush();
  const settingsRenders = [];
  hx.watchSettings({ changeDetectorRef: { detectChanges: () => settingsRenders.push(hx.summary()) } });

  replies.set("a:user/trackingoverview:get", () => Promise.resolve({ status: 200, trackings: ["A"] }));
  replies.set("b:user/trackingoverview:get", () => Promise.resolve({ status: 200, trackings: ["B"] }));
  const overview = await primary.request(message("user/trackingoverview", "get"));
  assert.deepEqual(overview.merged, { a: { status: 200, trackings: ["A"] }, b: { status: 200, trackings: ["B"] } });
  assert.equal(overview.commit, true);
  assert.equal(sockets.length, 2, "one secondary socket for the valid non-primary account");
  assert.equal(sockets[1].connectedAs, "b", "secondary socket opened with its own token");

  let overviewResolve;
  replies.set("a:user/trackingoverview:get", () => new Promise(resolve => { overviewResolve = resolve; }));
  const pendingOverview = primary.request(message("user/trackingoverview", "get"));
  hx.onServerData({ accountType: 2, token: token("a"), username: "alice", email: "" });
  overviewResolve({ status: 200, trackings: ["A"] });
  assert.equal((await pendingOverview).commit, true, "a settings refresh during an overview must not discard the merge");

  replies.delete("a:user/trackingoverview:get");
  replies.delete("b:user/trackingoverview:get");
  await assert.rejects(primary.request(message("user/trackingoverview", "get")), e => e.data.status === 400);

  allocations.push("a", "b");
  replies.set("a:track:addTracking", () => Promise.reject(new WebSocketRejectReason({ status: 200, error: { type: "maxTrackingReached" } })));
  replies.set("b:track:addTracking", () => Promise.resolve({ status: 200, added: "b" }));
  bridgeCalls.length = 0;
  assert.deepEqual(await primary.request(message("track", "addTracking", { trackingRequest: { asin: "X9" } })), { status: 200, added: "b" });
  assert.deepEqual(calls("applyAddResult").map(j => [j.accountId, j.ok]), [["a", false], ["b", true]]);

  allocations.push("a", "a");
  await assert.rejects(primary.request(message("track", "addTracking", { trackingRequest: { asin: "X8" } })),
    e => e.data.error.type === "maxTrackingReached" && e.data.maxTrackingAllowed === 600);

  replies.set("a:track:deleteTracking", m => Promise.resolve({ status: 200, deleted: m.asinBatch }));
  replies.set("b:track:deleteTracking", m => Promise.resolve({ status: 200, deleted: m.asinBatch }));
  bridgeCalls.length = 0;
  await primary.request(message("track", "deleteTracking", { asinBatch: ["X1", "X2", "X3"] }));
  assert.deepEqual(calls("applyDelete").map(j => j.accountId + ":" + j.asins.join(",")).sort(), ["a:X2,X3", "b:X1"]);

  store.owners.X4 = ["a", "b"];
  hx.applyPending();
  bridgeCalls.length = 0;
  await primary.request(message("track", "deleteTracking", { asinBatch: ["X4"] }));
  assert.deepEqual(calls("applyDelete").map(j => j.accountId + ":" + j.asins.join(",")).sort(), ["a:X4", "b:X4"], "a product tracked by two accounts is deleted from both");
  delete store.owners.X4;

  replies.set("b:track:addTracking", () => Promise.reject(new WebSocketRejectReason({ status: 401 })));
  await assert.rejects(primary.request(message("track", "addTracking", { trackingRequest: { asin: "X1" } })),
    e => e.data.status === 200 && /bob is signed out/.test(e.data.errorMsg), "editing an existing tracking reports the owner's failure instead of reallocating");
  store.accounts.accounts.find(a => a.id === "b").state = "ok";
  replies.set("b:track:addTracking", () => Promise.resolve({ status: 200, added: "b" }));

  replies.set("b:track:deleteTracking", () => Promise.reject(new WebSocketRejectReason({ status: 401 })));
  await assert.rejects(primary.request(message("track", "deleteTracking", { asinBatch: ["X1"] })),
    e => e.data.status === 200 && /bob is signed out/.test(e.data.errorMsg));
  assert.equal(store.accounts.accounts.find(a => a.id === "b").state, "invalid");

  bridgeFailure = "allocate: boom";
  await assert.rejects(primary.request(message("track", "addTracking", { trackingRequest: { asin: "X7" } })),
    e => e.data.status === 200 && /boom/.test(e.data.errorMsg));
  bridgeFailure = null;

  assert.equal(hx.route(primary, message("user/settings", "get")), null);
  navigation.url = "/login?type=forceLogout";
  assert.equal(hx.route(primary, message("user/settings", "removeFirebaseEndpoint", { key: "fcm" })), null, "logout keeps other accounts registered");
  navigation.url = "/manage";

  store.accounts.accounts.find(a => a.id === "b").state = "ok";
  store.pending = JSON.stringify({ op: "switch", id: "b" });
  const secondary = sockets[1];
  hx.applyPending();
  await flush();
  assert.equal(store.accounts.primaryId, "b");
  assert.equal(secondary.destroyed, true, "stale socket of the new primary account is closed");
  assert.equal(primary.connectedAs, "b", "primary socket reconnected as the new primary account");

  let logoutResolve;
  replies.set("a:user/session:logout", () => new Promise(resolve => { logoutResolve = resolve; }));
  replies.set("a:user/settings:removeFirebaseEndpoint", () => Promise.resolve({ status: 200 }));
  store.pending = JSON.stringify({ op: "remove", id: "a" });
  hx.applyPending();
  await flush();
  assert.ok(store.accounts.accounts.find(a => a.id === "a"), "account kept until its logout settles");
  logoutResolve({ status: 200 });
  await flush();
  assert.ok(!store.accounts.accounts.find(a => a.id === "a"), "account removed after logout settled");

  store.accounts.accounts.push({ id: "a", username: "alice", token: token("a"), state: "ok", limit: 200, tracked: 1 });
  store.pending = JSON.stringify({ op: "addInProgress", previousPrimaryId: "b" });
  primary.storageService.settings = { token: "", accountType: 0 };
  navigation.events.forEach(h => h({ constructor: { name: "NavigationEnd" }, urlAfterRedirects: "/home" }));
  await flush();
  assert.equal(store.pending, "", "abandoned add is cleared");
  store.accounts.accounts[0].tracked = 9;
  const before = settingsRenders.length;
  hx.applyPending();
  await flush();
  assert.equal(settingsRenders.length, before, "an unchanged summary does not re-render settings");
  const originalSnapshot = snapshot;
  snapshot = () => ({ ...originalSnapshot(), summary: "changed" });
  hx.applyPending();
  await flush();
  snapshot = originalSnapshot;
  assert.deepEqual(settingsRenders.slice(before), ["changed"], "a changed summary re-renders the settings page");
  assert.equal(primary.storageService.settings.token, token("b"), "previous account restored into Hawk");

  primary.storageService.settings = { token: token("b"), accountType: 2, username: "bob", pushToken: "fcm" };
  hx.applyPending();
  replies.set("a:user/trackingoverview:get", () => Promise.resolve({ status: 200, trackings: ["A"] }));
  replies.set("b:user/trackingoverview:get", () => Promise.resolve({ status: 200, trackings: ["B"] }));
  await primary.request(message("user/trackingoverview", "get"));
  const staleAlice = sockets.find(s => s.hxAccountId === "a" && !s.destroyed);
  assert.ok(staleAlice, "alice has a secondary socket before she signs in again");
  primary.storageService.settings = { token: "", accountType: 0 };
  assert.equal(hx.route(primary, message("user/trackingoverview", "get")), null, "no routing while the app session is anonymous");
  hx.onServerData({ accountType: 2, token: token("x"), username: "alice", email: "" });
  assert.equal(staleAlice.destroyed, true, "signing alice in again closes her stale socket");
  primary.storageService.settings = { token: token("x"), accountType: 2, username: "alice", pushToken: "fcm" };
  hx.applyPending();

  store.accounts.accounts = store.accounts.accounts.filter(a => a.id !== "c");
  assert.deepEqual(hx.productTag("X1"), { below: "bob" });
  store.accounts.tagPlacement = "flag";
  hx.applyPending();
  assert.deepEqual(hx.productTag("X1"), { flag: "bob" });
  store.accounts.tagPlacement = "off";
  hx.applyPending();
  assert.deepEqual(hx.productTag("X1"), {});

  store.accounts.accounts = store.accounts.accounts.filter(a => a.id === "a");
  store.accounts.primaryId = "a";
  hx.applyPending();
  assert.equal(hx.onReset(), false, "a single-account logout keeps stock behaviour");
  assert.deepEqual(store.accounts.accounts, [], "the logged-out only account is removed from the store");
  console.log("accounts.js harness passed");
})().catch(error => { console.error(error); process.exit(1); });
