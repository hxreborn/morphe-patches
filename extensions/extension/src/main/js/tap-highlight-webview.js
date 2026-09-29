(function () {
  var styleId = "hx-tap-highlight";
  if (document.getElementById(styleId)) return;
  var styleElement = document.createElement("style");
  styleElement.id = styleId;
  styleElement.textContent = "*{-webkit-tap-highlight-color:transparent !important;}";
  (document.head || document.documentElement).appendChild(styleElement);
})();
