(function () {
  var styleId = "hx-material-switch";
  if (document.getElementById(styleId)) return;
  var container = ".toggle-container".repeat(5);
  var checked = ".toggle-container--checked".repeat(5);
  var icon = " > .toggle-container-text";
  var rules = [
    ".toggle-label.toggle-label{max-block-size:32px !important;}",
    container + "{color-scheme:dark only !important;inline-size:52px !important;min-block-size:32px !important;" +
      "max-block-size:32px !important;border-radius:16px !important;" +
      "background-color:color-mix(in srgb,var(--text-weak) 20%,transparent) !important;" +
      "transition:background-color .2s ease-out !important;}",
    container + "::after{inline-size:52px !important;border-radius:16px !important;" +
      "border:2px solid var(--text-weak) !important;box-shadow:none !important;}",
    container + "::before{inset-block-start:4px !important;inset-block-end:auto !important;inset-inline-start:4px !important;" +
      "inline-size:24px !important;block-size:24px !important;border-radius:50% !important;" +
      "background-color:var(--text-weak) !important;" +
      "transition:transform .2s ease-out,inset .1s ease-out,inline-size .1s ease-out,block-size .1s ease-out," +
      "background-color .2s ease-out !important;}",
    container + ":active::before{inset-block-start:2px !important;inset-inline-start:2px !important;" +
      "inline-size:28px !important;block-size:28px !important;}",
    container + icon + "{inset-block-start:4px !important;inset-block-end:auto !important;inline-size:24px !important;" +
      "block-size:24px !important;z-index:3 !important;}",
    container + icon + " .toggle-container-img{display:block !important;transition:opacity .15s ease-out !important;}",
    container + icon + ":first-of-type{inset-inline-start:4px !important;inset-inline-end:auto !important;" +
      "color:var(--background-weak) !important;}",
    container + icon + ":last-of-type{inset-inline-end:4px !important;inset-inline-start:auto !important;" +
      "color:var(--interaction-norm) !important;}",
    container + icon + ":first-of-type .toggle-container-img{opacity:1 !important;}",
    container + icon + ":last-of-type .toggle-container-img{opacity:0 !important;}",
    checked + "{background-color:var(--interaction-norm) !important;}",
    checked + "::after{border-color:transparent !important;}",
    checked + "::before{background-color:__CHECKED_THUMB__ !important;transform:translateX(20px) !important;}",
    "[dir=rtl] " + checked + "::before{transform:translateX(-20px) !important;}",
    checked + icon + ":first-of-type .toggle-container-img{opacity:0 !important;}",
    checked + icon + ":last-of-type .toggle-container-img{opacity:1 !important;}"
  ];
  var styleElement = document.createElement("style");
  styleElement.id = styleId;
  styleElement.textContent = rules.join("");
  (document.head || document.documentElement).appendChild(styleElement);
})();
