(function () {
  var colorMap = __TONES__,
    hueShift = __SHIFT__,
    saturationScale = __SATURATION__,
    styleId = "hx-accent",
    amoledStyleId = "hx-amoled",
    stylesheetCount = -1,
    updatePending = 0;
  var minHue = 235,
    maxHue = 285,
    minSaturation = 0.45;
  var pollIntervalMs = 250,
    pollWindowMs = 5 * 60 * 1000,
    pollDeadline = Date.now() + pollWindowMs;
  function isInjectedStyle(node) {
    return node && (node.id === styleId || node.id === amoledStyleId);
  }
  function channelToHex(channel) {
    channel = Number(channel).toString(16);
    return channel.length < 2 ? "0" + channel : channel;
  }
  function rgbToHsl(red, green, blue) {
    red /= 255;
    green /= 255;
    blue /= 255;
    var maxChannel = Math.max(red, green, blue),
      minChannel = Math.min(red, green, blue),
      lightness = (maxChannel + minChannel) / 2,
      channelRange = maxChannel - minChannel,
      hue = 0,
      saturation = 0;
    if (channelRange > 0) {
      saturation =
        lightness > 0.5
          ? channelRange / (2 - maxChannel - minChannel)
          : channelRange / (maxChannel + minChannel);
      hue =
        maxChannel == red
          ? (green - blue) / channelRange + (green < blue ? 6 : 0)
          : maxChannel == green
            ? (blue - red) / channelRange + 2
            : (red - green) / channelRange + 4;
      hue *= 60;
    }
    return [hue, saturation, lightness];
  }
  function hueToRgbChannel(lowerChannel, upperChannel, hueFraction) {
    if (hueFraction < 0) hueFraction += 1;
    if (hueFraction > 1) hueFraction -= 1;
    if (hueFraction < 1 / 6)
      return lowerChannel + (upperChannel - lowerChannel) * 6 * hueFraction;
    if (hueFraction < 0.5) return upperChannel;
    if (hueFraction < 2 / 3)
      return (
        lowerChannel + (upperChannel - lowerChannel) * (2 / 3 - hueFraction) * 6
      );
    return lowerChannel;
  }
  function hslToRgb(hue, saturation, lightness) {
    hue = (((hue % 360) + 360) % 360) / 360;
    if (saturation <= 0)
      return [lightness * 255, lightness * 255, lightness * 255];
    var upperChannel =
        lightness < 0.5
          ? lightness * (1 + saturation)
          : lightness + saturation - lightness * saturation,
      lowerChannel = 2 * lightness - upperChannel;
    return [
      Math.round(
        hueToRgbChannel(lowerChannel, upperChannel, hue + 1 / 3) * 255,
      ),
      Math.round(hueToRgbChannel(lowerChannel, upperChannel, hue) * 255),
      Math.round(
        hueToRgbChannel(lowerChannel, upperChannel, hue - 1 / 3) * 255,
      ),
    ];
  }
  function recolorRgb(red, green, blue) {
    var mappedRgb = colorMap[red + "," + green + "," + blue];
    if (mappedRgb) return mappedRgb.split(",");
    var hslColor = rgbToHsl(red, green, blue);
    if (
      hslColor[1] < minSaturation ||
      hslColor[0] < minHue ||
      hslColor[0] > maxHue
    )
      return null;
    return hslToRgb(
      hslColor[0] + hueShift,
      Math.min(1, hslColor[1] * saturationScale),
      hslColor[2],
    );
  }
  function recolorCssValue(cssValue) {
    var changed = false;
    cssValue = cssValue.replace(
      /#([0-9a-fA-F]{3,8})\b/g,
      function (matchedColor, hexDigits) {
        var red,
          green,
          blue,
          alphaHex = "";
        if (hexDigits.length == 3 || hexDigits.length == 4) {
          red = parseInt(hexDigits[0] + hexDigits[0], 16);
          green = parseInt(hexDigits[1] + hexDigits[1], 16);
          blue = parseInt(hexDigits[2] + hexDigits[2], 16);
          if (hexDigits.length == 4) alphaHex = hexDigits[3] + hexDigits[3];
        } else if (hexDigits.length == 6 || hexDigits.length == 8) {
          red = parseInt(hexDigits.substr(0, 2), 16);
          green = parseInt(hexDigits.substr(2, 2), 16);
          blue = parseInt(hexDigits.substr(4, 2), 16);
          if (hexDigits.length == 8) alphaHex = hexDigits.substr(6, 2);
        } else return matchedColor;
        var recoloredRgb = recolorRgb(red, green, blue);
        if (!recoloredRgb) return matchedColor;
        changed = true;
        return (
          "#" +
          channelToHex(recoloredRgb[0]) +
          channelToHex(recoloredRgb[1]) +
          channelToHex(recoloredRgb[2]) +
          alphaHex
        );
      },
    );
    cssValue = cssValue.replace(
      /(\d{1,3})(\s*[, ]\s*)(\d{1,3})(\s*[, ]\s*)(\d{1,3})/g,
      function (
        matchedColor,
        red,
        firstSeparator,
        green,
        secondSeparator,
        blue,
      ) {
        var recoloredRgb = recolorRgb(+red, +green, +blue);
        if (!recoloredRgb) return matchedColor;
        changed = true;
        return (
          recoloredRgb[0] +
          firstSeparator +
          recoloredRgb[1] +
          secondSeparator +
          recoloredRgb[2]
        );
      },
    );
    return changed ? cssValue : null;
  }
  function appendRecoloredRules(rule, outputRules) {
    if (rule.styleSheet) return;
    if (rule.cssRules && rule.cssRules.length) {
      var nestedRules = [],
        i;
      for (i = 0; i < rule.cssRules.length; i++)
        appendRecoloredRules(rule.cssRules[i], nestedRules);
      if (nestedRules.length) {
        var atRuleHeader = rule.cssText ? rule.cssText.split("{")[0] : "";
        if (atRuleHeader)
          outputRules.push(atRuleHeader + "{" + nestedRules.join("") + "}");
      }
      return;
    }
    var style = rule.style;
    if (!style || !rule.selectorText) return;
    var declarations = "",
      i;
    for (i = 0; i < style.length; i++) {
      var propertyName = style[i],
        recoloredValue = recolorCssValue(style.getPropertyValue(propertyName));
      if (recoloredValue !== null)
        declarations += propertyName + ":" + recoloredValue + " !important;";
    }
    if (declarations)
      outputRules.push(rule.selectorText + "{" + declarations + "}");
  }
  function updateAccentStyles(force) {
    var stylesheets = document.styleSheets;
    if (!force && stylesheets.length === stylesheetCount) return;
    stylesheetCount = stylesheets.length;
    var outputRules = [],
      i,
      j;
    for (i = 0; i < stylesheets.length; i++) {
      if (isInjectedStyle(stylesheets[i].ownerNode)) continue;
      var rules;
      try {
        rules = stylesheets[i].cssRules;
      } catch (e) {
        continue;
      }
      if (!rules) continue;
      for (j = 0; j < rules.length; j++)
        appendRecoloredRules(rules[j], outputRules);
    }
    if (!outputRules.length) return;
    var head = document.head || document.documentElement;
    var styleElement = document.getElementById(styleId);
    if (!styleElement) {
      styleElement = document.createElement("style");
      styleElement.id = styleId;
    }
    styleElement.textContent = outputRules.join("");
    if (styleElement.parentNode !== head || styleElement.nextSibling)
      head.appendChild(styleElement);
  }
  function scheduleUpdate() {
    if (updatePending) return;
    updatePending = 1;
    var apply = function () {
      updatePending = 0;
      updateAccentStyles(true);
    };
    if (window.requestAnimationFrame) requestAnimationFrame(apply);
    else setTimeout(apply, 0);
  }
  function onDocumentMutated(records) {
    var i, j, node, tag;
    for (i = 0; i < records.length; i++)
      for (j = 0; j < records[i].addedNodes.length; j++) {
        node = records[i].addedNodes[j];
        tag = node.tagName;
        if (!tag) continue;
        tag = tag.toUpperCase();
        if (tag !== "LINK" && tag !== "STYLE") continue;
        if (isInjectedStyle(node)) continue;
        if (node.addEventListener)
          node.addEventListener("load", scheduleUpdate);
        scheduleUpdate();
      }
  }
  updateAccentStyles(true);
  if (window.MutationObserver)
    new MutationObserver(onDocumentMutated).observe(document.documentElement, {
      childList: true,
      subtree: true,
    });
  var timer = setInterval(function () {
    if (Date.now() > pollDeadline) {
      clearInterval(timer);
      return;
    }
    updateAccentStyles(false);
  }, pollIntervalMs);
})();
