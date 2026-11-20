(function () {
  if (window.hxLumoPlus) return;
  window.hxLumoPlus = true;
  var json = Response.prototype.json;
  Response.prototype.json = function () {
    return json.call(this).then(function (data) {
      var user = data && data.User;
      if (user && user.NumLumo === 0) user.NumLumo = 1;
      return data;
    });
  };
  function hideUpgradePrompts() {
    var style = document.createElement("style");
    style.textContent = ".lumo-upgrade-trigger,.manage-plan{display:none !important;}";
    document.head.appendChild(style);
  }
  if (document.head && document.readyState !== "loading") {
    hideUpgradePrompts();
  } else {
    window.addEventListener("DOMContentLoaded", hideUpgradePrompts);
  }
})();
