const hxAccountsRuntime = (() => {
  const BRIDGE_CLASS = "app.hxreborn.extension.keepa.AccountBridge";
  const SESSION_TOKEN_LENGTH = 64;
  const SIGNED_IN_ACCOUNT_TYPE = 2;
  const ANONYMOUS_ACCOUNT_TYPE = 0;
  const STATUS_OK = 200;
  const STATUS_UNAUTHORIZED = 401;
  const STATUS_RATE_LIMITED = 429;
  const LOGIN_ROUTE = "/login";
  const FORCE_LOGOUT_QUERY = "forceLogout";
  const MODULES = {
    nativeHelper: "./node_modules/@nativescript/core/utils/native-helper.android.js",
    message: "./src/app/services/network/web-socket-message.ts",
    rejectReason: "./src/app/services/network/web-socket-reject-reason.ts",
    settings: "./src/app/services/settings/settings.service.ts",
    push: "./src/app/services/push/push.service.ts",
  };

  let primaryService = null,
    injector = null,
    context = null,
    bridgeMethod = null,
    constructing = null,
    settingsComponent = null,
    ownershipRevision = 0;
  let state = {
    accounts: { primaryId: "", allocation: "auto", accounts: [] },
    owners: {},
    pending: null,
    summary: "",
    limitTotal: 0,
  };
  const transports = new Map();
  const routed = new WeakSet();
  const notificationOwners = {};

  function logFailure(where, err) {
    const detail = err instanceof Error ? err.stack || err.message : JSON.stringify(err);
    console.error("HX " + where + ": " + detail);
  }
  function exportNamed(exports, name) {
    const found = Object.values(exports).find((x) => typeof x === "function" && x.name === name);
    if (!found) throw new Error("export " + name + " not found");
    return found;
  }
  function moduleExport(path, name) {
    return exportNamed(hxRequire(path), name);
  }
  const WebSocketMessage = () => moduleExport(MODULES.message, "WebSocketMessage");
  const RejectReason = () => moduleExport(MODULES.rejectReason, "WebSocketRejectReason");
  function injectService(path, name) {
    return injector.get(moduleExport(path, name));
  }
  function readableRejection(text) {
    return new (RejectReason())({ status: STATUS_OK, errorMsg: "Accounts: " + text });
  }

  function appContext() {
    if (!context) {
      const helper = hxRequire(MODULES.nativeHelper);
      const key = Object.keys(helper).find((k) => helper[k] && typeof helper[k].getApplicationContext === "function");
      context = helper[key].getApplicationContext();
    }
    return context;
  }
  function callJava(method, args) {
    if (!bridgeMethod)
      bridgeMethod = java.lang.Class.forName(BRIDGE_CLASS).getMethod("call", [
        android.content.Context.class,
        java.lang.String.class,
        java.lang.String.class,
      ]);
    const reply = JSON.parse(
      String(bridgeMethod.invoke(null, [appContext(), method, args === undefined ? "" : JSON.stringify(args)])),
    );
    if (reply.error) throw new Error(reply.error);
    return reply.value;
  }
  function renderSettingsSummary() {
    if (!settingsComponent) return;
    try {
      settingsComponent.changeDetectorRef.detectChanges();
    } catch (err) {
      settingsComponent = null;
    }
  }
  function takeSnapshot(snapshot) {
    const previousSummary = state.summary;
    state = {
      accounts: snapshot.accounts,
      owners: snapshot.owners,
      pending: snapshot.pending || null,
      summary: snapshot.summary || "",
      limitTotal: snapshot.limitTotal || 0,
    };
    if (state.summary !== previousSummary) Promise.resolve().then(renderSettingsSummary);
  }
  function refresh() {
    takeSnapshot(callJava("load"));
  }
  function update(method, args) {
    takeSnapshot(callJava(method, args));
  }
  function updateOwnership(method, args) {
    ownershipRevision++;
    update(method, args);
  }

  function list() {
    return state.accounts.accounts;
  }
  function byId(id) {
    return list().find((account) => account.id === id);
  }
  function primaryId() {
    return state.accounts.primaryId;
  }
  function primaryAccount() {
    return byId(primaryId());
  }
  function ownerIds(asin) {
    const owners = state.owners[asin];
    return Array.isArray(owners) ? owners : [];
  }
  function validAccounts() {
    return list().filter((account) => account.state !== "invalid");
  }
  function isSessionToken(token) {
    return typeof token === "string" && token.length === SESSION_TOKEN_LENGTH;
  }
  function appStorage() {
    return primaryService.storageService;
  }
  function onLoginRoute() {
    return String(primaryService.navigationService.getCurrentUrl()).indexOf(LOGIN_ROUTE) === 0;
  }

  function secondaryStorage(real, id) {
    return {
      getSettings() {
        const settings = real.getSettings();
        const account = byId(id);
        settings.token = account ? account.token : "";
        settings.accountType = SIGNED_IN_ACCOUNT_TYPE;
        if (account) {
          settings.username = account.username;
          settings.email = account.email;
        }
        return settings;
      },
      setSettings(settings) {
        const account = byId(id);
        if (account && settings && isSessionToken(settings.token) && settings.token !== account.token)
          update("upsertToken", { id: id, token: settings.token });
        return true;
      },
      getDefaultSettings() {
        return real.getDefaultSettings();
      },
    };
  }

  function turnstileOwner(requestId) {
    const services = [primaryService, ...transports.values()];
    return (
      services.find((candidate) => candidate.deferredStorage.has(requestId)) ||
      services.find((candidate) => candidate.awaitingTurnstile) ||
      primaryService
    );
  }
  function scopeTurnstile(service) {
    const obtained = service.handleTurnstileTokenObtained,
      failed = service.handleTurnstileVerificationFailed;
    service.handleTurnstileTokenObtained = function (id, token) {
      if (turnstileOwner(id) === this) return obtained.call(this, id, token);
    };
    service.handleTurnstileVerificationFailed = function (id, err) {
      if (turnstileOwner(id) === this) failed.call(this, id, err);
    };
  }

  function transport(account) {
    if (account.id === primaryId()) return primaryService;
    let created = transports.get(account.id);
    if (!created) {
      constructing = account.id;
      try {
        created = injector.runInContext(() => new primaryService.constructor());
      } finally {
        constructing = null;
      }
      transports.set(account.id, created);
    }
    return created;
  }
  function closeTransport(id) {
    const closing = transports.get(id);
    if (closing) {
      transports.delete(id);
      closing.isAppInForeground = false;
      closing.ngOnDestroy();
    }
  }

  function markRouted(message) {
    routed.add(message);
    return message;
  }
  function clone(src, extra) {
    return markRouted(Object.assign(Object.create(Object.getPrototypeOf(src)), src, extra));
  }
  function newMessage(path, type, fields) {
    return markRouted(Object.assign(new (WebSocketMessage())(path, type), fields));
  }

  function convertRejection(account, err) {
    const status = err && err.data && err.data.status;
    if (status === STATUS_UNAUTHORIZED || status === STATUS_RATE_LIMITED)
      update("applyReject", { accountId: account.id, status: status });
    const current = byId(account.id);
    if (account.id === primaryId() || !current || current.state === "ok") return err;
    return readableRejection(account.username + (current.state === "invalid" ? " is signed out" : " is rate limited"));
  }
  function sendOn(account, src, extra) {
    return Promise.resolve(transport(account).request(clone(src, extra))).catch((err) => {
      throw convertRejection(account, err);
    });
  }

  function fanOut(src) {
    const responses = {},
      failures = [];
    return Promise.all(
      validAccounts().map((account) =>
        sendOn(account, src).then(
          (response) => {
            responses[account.id] = response;
          },
          (err) => {
            failures.push(err);
          },
        ),
      ),
    ).then(() => {
      if (failures.length && !Object.keys(responses).length) throw failures[0];
      failures.forEach((err) => logFailure("fan-out " + src.path, err));
      return responses;
    });
  }

  function mergeOverviews(responses, startRevision) {
    const merged = callJava("mergeOverviews", { responses: responses, commit: startRevision === ownershipRevision });
    refresh();
    return merged;
  }
  function routeOverview(src) {
    const startRevision = ownershipRevision;
    if (list().length < 2) {
      const id = primaryId();
      return Promise.resolve(primaryService.request(markRouted(src))).then((response) => {
        mergeOverviews({ [id]: response }, startRevision);
        return response;
      });
    }
    return fanOut(src).then((responses) => mergeOverviews(responses, startRevision));
  }

  function fanOutNotifications(src) {
    return fanOut(src).then((responses) => {
      const notifications = [];
      for (const id in responses) {
        const response = responses[id];
        if (Array.isArray(response.notifications))
          for (const notification of response.notifications) {
            if (notification.notificationId != null) notificationOwners[notification.notificationId] = id;
            notifications.push(notification);
          }
      }
      notifications.sort((first, second) => (second.createDate || 0) - (first.createDate || 0));
      return { status: STATUS_OK, notifications: notifications.slice(0, src.perPage || notifications.length) };
    });
  }

  function routeGetTracking(src) {
    const owner = byId(ownerIds(src.asin)[0]);
    if (owner) return sendOn(owner, src);
    return fanOut(src).then((responses) => {
      for (const account of list()) {
        const response = responses[account.id];
        if (response && Array.isArray(response.trackings) && response.trackings.length) {
          updateOwnership("applyAddResult", { asin: src.asin, accountId: account.id, ok: true });
          return response;
        }
      }
      return { status: STATUS_OK, trackings: null };
    });
  }

  function routeAdd(src) {
    const asin = src.trackingRequest && src.trackingRequest.asin;
    if (!asin) return null;
    const recordAdd = (account) => (response) => {
      updateOwnership("applyAddResult", { asin: asin, accountId: account.id, ok: true });
      return response;
    };
    const owner = byId(ownerIds(asin)[0]);
    if (owner) return sendOn(owner, src).then(recordAdd(owner));
    const tried = [];
    const attempt = () => {
      const account = byId(callJava("allocate", { asin: asin }));
      if (!account || tried.includes(account.id))
        return Promise.reject(
          new (RejectReason())({
            status: STATUS_OK,
            error: { type: "maxTrackingReached" },
            maxTrackingAllowed: state.limitTotal,
          }),
        );
      tried.push(account.id);
      return sendOn(account, src).then(recordAdd(account), (err) => {
        const full = !!(err && err.data && err.data.error && err.data.error.type === "maxTrackingReached");
        const current = byId(account.id);
        if (!full && current && current.state === "ok") throw err;
        if (full)
          updateOwnership("applyAddResult", {
            asin: asin,
            accountId: account.id,
            ok: false,
            errorType: "maxTrackingReached",
          });
        return attempt();
      });
    };
    return attempt();
  }

  function routeDelete(src) {
    const asins = src.asinBatch || (src.asin ? [src.asin] : []);
    if (!asins.length) return null;
    const groups = new Map();
    for (const asin of asins) {
      const owners = ownerIds(asin).map(byId).filter(Boolean);
      for (const account of owners.length ? owners : [primaryAccount()]) {
        if (!groups.has(account)) groups.set(account, []);
        groups.get(account).push(asin);
      }
    }
    return Promise.allSettled(
      [...groups].map(([account, batch]) =>
        sendOn(account, src, { asinBatch: batch }).then((response) => {
          updateOwnership("applyDelete", { accountId: account.id, asins: batch });
          return response;
        }),
      ),
    ).then((results) => {
      const failed = results.find((result) => result.status === "rejected");
      if (failed) throw failed.reason;
      return results[0].value;
    });
  }

  function routeNotificationDelete(src) {
    return sendOn(byId(notificationOwners[src.notificationId]) || primaryAccount(), src);
  }

  function fanOutFirebase(src) {
    return fanOut(src).then(() => ({ status: STATUS_OK }));
  }

  function dispatch(msg) {
    if (appStorage().getSettings().accountType !== SIGNED_IN_ACCOUNT_TYPE) return null;
    const path = msg.path,
      type = msg.type;
    if (path === "user/trackingoverview" && list().length >= 1) return routeOverview(msg);
    if (list().length < 2) return null;
    if (path === "track" && type === "getTracking") return routeGetTracking(msg);
    if (path === "track" && type === "addTracking") return routeAdd(msg);
    if (path === "track" && type === "deleteTracking") return routeDelete(msg);
    if (path === "user/notification" && type === "website") return fanOutNotifications(msg);
    if (path === "user/notification" && type === "delete") return routeNotificationDelete(msg);
    if (path === "user/settings" && type === "setFirebaseEndpoint") return fanOutFirebase(msg);
    if (path === "user/settings" && type === "removeFirebaseEndpoint" && !onLoginRoute()) return fanOutFirebase(msg);
    return null;
  }

  function route(service, msg) {
    if (service.hxSecondary || routed.has(msg) || msg.turnstile) return null;
    let outcome;
    try {
      outcome = dispatch(msg);
    } catch (err) {
      outcome = Promise.reject(err);
    }
    return (
      outcome &&
      outcome.catch((err) => {
        if (err instanceof RejectReason()) throw err;
        logFailure("route " + msg.path, err);
        throw readableRejection(String((err && err.message) || err));
      })
    );
  }

  function writeAppStorage(account) {
    const settings = appStorage().getSettings();
    settings.token = account ? account.token : "";
    settings.accountType = account ? SIGNED_IN_ACCOUNT_TYPE : ANONYMOUS_ACCOUNT_TYPE;
    settings.username = account ? account.username : "";
    settings.email = account ? account.email : "";
    appStorage().setSettings(settings, false);
  }
  function makePrimary(id) {
    ownershipRevision++;
    closeTransport(id);
    writeAppStorage(byId(id));
    primaryService.reconnect(true);
    return Promise.resolve(injectService(MODULES.settings, "SettingsService").load());
  }
  function syncPrimaryToken() {
    const settings = appStorage().getSettings();
    const primary = primaryAccount();
    if (
      primary &&
      settings.accountType === SIGNED_IN_ACCOUNT_TYPE &&
      settings.username === primary.username &&
      isSessionToken(settings.token) &&
      settings.token !== primary.token
    )
      update("upsertToken", { id: primary.id, token: settings.token });
  }

  function applyPending() {
    refresh();
    syncPrimaryToken();
    const op = state.pending;
    if (!op || op.op === "addInProgress") return;
    update("clearPending");
    if (op.op === "add" || op.op === "reauth") {
      update("setPending", { op: "addInProgress", previousPrimaryId: primaryId() });
      writeAppStorage(null);
      primaryService.reconnect(true);
      primaryService.navigationService.goToLogin();
    } else if (op.op === "switch") {
      updateOwnership("setPrimary", { id: op.id });
      makePrimary(op.id).catch((err) => logFailure("switch", err));
    } else if (op.op === "remove") {
      removeAccount(op.id).catch((err) => logFailure("remove", err));
    }
  }

  function restoreAbandonedAdd() {
    refresh();
    const op = state.pending;
    if (!op || op.op !== "addInProgress" || appStorage().getSettings().accountType === SIGNED_IN_ACCOUNT_TYPE) return;
    update("clearPending");
    const previous = byId(op.previousPrimaryId);
    if (previous) {
      updateOwnership("setPrimary", { id: previous.id });
      makePrimary(previous.id).catch((err) => logFailure("restore", err));
    }
  }

  function removeAccount(id) {
    const account = byId(id);
    if (!account) return Promise.resolve();
    const socket = transport(account);
    const push = appStorage().getSettings().pushToken;
    const cleanup = [];
    if (account.state !== "invalid") {
      if (push) cleanup.push(socket.request(newMessage("user/settings", "removeFirebaseEndpoint", { key: push })));
      cleanup.push(socket.request(newMessage("user/session", "logout", { token: account.token })));
    }
    return Promise.allSettled(cleanup).then((results) => {
      results.forEach((result) => {
        if (result.status === "rejected") logFailure("remove cleanup " + account.username, result.reason);
      });
      const wasPrimary = id === primaryId();
      updateOwnership("remove", { id: id });
      closeTransport(id);
      if (!wasPrimary) return;
      if (primaryId()) return makePrimary(primaryId());
      appStorage().setSettings(appStorage().getDefaultSettings(), true);
      primaryService.reconnect(true);
      primaryService.navigationService.goToLogin();
    });
  }

  function onServerData(settings) {
    if (settings.accountType !== SIGNED_IN_ACCOUNT_TYPE || !isSessionToken(settings.token)) return;
    update("onServerData", { token: settings.token, username: settings.username, email: settings.email || "" });
    closeTransport(primaryId());
    if (state.pending && state.pending.op === "addInProgress") update("clearPending");
  }

  function onReset() {
    refresh();
    if (list().length < 2) {
      if (list().length) updateOwnership("remove", { id: primaryId() });
      return false;
    }
    const previous = primaryId();
    const forced = String(primaryService.navigationService.getCurrentUrl()).indexOf(FORCE_LOGOUT_QUERY) >= 0;
    const hadPush = !!appStorage().getSettings().pushToken;
    if (forced) {
      update("invalidate", { id: previous });
      const next = validAccounts()[0];
      if (!next) return false;
      updateOwnership("setPrimary", { id: next.id });
    } else {
      updateOwnership("remove", { id: previous });
      if (!primaryId()) return false;
    }
    makePrimary(primaryId())
      .then(() => {
        if (onLoginRoute()) primaryService.navigationService.goToHomeTab();
        if (hadPush) return injectService(MODULES.push, "PushService").proactivelyCheckAndRegisterIfEnabled();
      })
      .catch((err) => logFailure("reset", err));
    return true;
  }

  function onForceLogout(service) {
    if (!service.hxSecondary) return false;
    if (service.hxAccountId) update("applyReject", { accountId: service.hxAccountId, status: STATUS_UNAUTHORIZED });
    service.flushAllRequests();
    if (transports.get(service.hxAccountId) === service) closeTransport(service.hxAccountId);
    return true;
  }

  function watchNavigation() {
    primaryService.navigationService._router.events.subscribe((event) => {
      if (event.constructor.name !== "NavigationEnd" || String(event.urlAfterRedirects).indexOf(LOGIN_ROUTE) === 0)
        return;
      try {
        restoreAbandonedAdd();
      } catch (err) {
        logFailure("navigation", err);
      }
    });
  }

  function guarded(where, fn, fallback) {
    return (...a) => {
      try {
        return fn(...a);
      } catch (err) {
        logFailure(where, err);
        return fallback;
      }
    };
  }

  return {
    attach: guarded("attach", (service, inject, core) => {
      scopeTurnstile(service);
      if (constructing !== null) {
        service.hxSecondary = true;
        service.hxAccountId = constructing;
        service.storageService = secondaryStorage(service.storageService, constructing);
        return;
      }
      for (const id of [...transports.keys()]) closeTransport(id);
      primaryService = service;
      injector = inject(exportNamed(core, "EnvironmentInjector"));
      refresh();
      watchNavigation();
      Promise.resolve()
        .then(applyPending)
        .catch((err) => logFailure("applyPending", err));
    }),
    route,
    onServerData: guarded("onServerData", onServerData),
    onReset: guarded("onReset", onReset, false),
    onForceLogout: guarded("onForceLogout", onForceLogout, false),
    applyPending: guarded("applyPending", applyPending),
    openAccounts: guarded("openAccounts", () => {
      callJava("openAccounts");
    }),
    summary() {
      return state.summary;
    },
    watchSettings(component) {
      settingsComponent = component;
    },
    productTag(asin) {
      const placement = state.accounts.tagPlacement || "below";
      if (list().length < 2 || placement === "off") return {};
      const owner = byId(ownerIds(asin)[0]);
      return owner ? { [placement]: owner.username } : {};
    },
  };
})();
globalThis.hxAccounts = hxAccountsRuntime;
