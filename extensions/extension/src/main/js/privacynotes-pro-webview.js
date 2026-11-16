(function () {
  if (window.__hxProUnlock) return;
  window.__hxProUnlock = true;
  var originalFetch = window.fetch;
  if (typeof originalFetch !== "function") return;
  var REGISTER_DEVICE = "/functions/v1/register-device";
  var IS_PRO = "/rest/v1/rpc/is_pro";
  var HEARTBEAT = "/rest/v1/rpc/device_heartbeat";
  var UNREGISTERED_KEY = "hx.unregisteredDevice";
  var HTTP_OK = 200;
  function pathOf(input) {
    try {
      var raw = typeof input === "string" ? input : input && input.url ? input.url : String(input);
      return new URL(raw, location.href).pathname;
    } catch (ignored) {
      return "";
    }
  }
  function jsonResponse(body) {
    return new Response(JSON.stringify(body), {
      status: HTTP_OK,
      headers: { "Content-Type": "application/json" },
    });
  }
  function setUnregistered(unregistered) {
    try {
      if (unregistered) localStorage.setItem(UNREGISTERED_KEY, "1");
      else localStorage.removeItem(UNREGISTERED_KEY);
    } catch (ignored) {}
  }
  function isUnregistered() {
    try {
      return localStorage.getItem(UNREGISTERED_KEY) === "1";
    } catch (ignored) {
      return false;
    }
  }
  function unlockRegistration(response) {
    return response
      .clone()
      .json()
      .then(function (data) {
        if (response.ok && data && typeof data === "object") {
          setUnregistered(false);
          data.is_pro = true;
          return jsonResponse(data);
        }
        if (data && data.error === "device_limit_reached") {
          setUnregistered(true);
          return jsonResponse({ is_pro: true });
        }
        return response;
      })
      .catch(function () {
        return response;
      });
  }
  window.fetch = function (input) {
    var path = pathOf(input);
    var promise = originalFetch.apply(this, arguments);
    if (path === IS_PRO) {
      return promise.then(function (response) {
        return response.ok ? jsonResponse(true) : response;
      });
    }
    if (path === HEARTBEAT) {
      return promise.then(function (response) {
        return response.ok && isUnregistered() ? jsonResponse(true) : response;
      });
    }
    if (path === REGISTER_DEVICE) return promise.then(unlockRegistration);
    return promise;
  };
})();
