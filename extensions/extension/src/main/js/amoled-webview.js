(function () {
  var styleId = "hx-amoled";
  if (document.getElementById(styleId)) return;
  var styleElement = document.createElement("style");
  styleElement.id = styleId;
  styleElement.textContent =
    "html,body,.mobile-settings.mobile-settings{background-color:#000000 !important;}";
  (document.head || document.documentElement).appendChild(styleElement);
})();
