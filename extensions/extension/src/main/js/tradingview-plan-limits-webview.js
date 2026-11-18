(function () {
  if (window.hxPlanLimits) return;
  window.hxPlanLimits = true;
  Object.defineProperty(window, 'WEBSOCKET_HOST', {
    configurable: true,
    get: function () { return 'widgetdata.tradingview.com'; },
    set: function () {}
  });
  var originalParse = JSON.parse;
  JSON.parse = function () {
    var plans = originalParse.apply(this, arguments);
    if (!plans || !plans.free || !plans.free.MULTIPLE_CHARTS) return plans;
    var extended = {};
    var planName;
    for (planName in plans) {
      if (plans[planName] && plans[planName].extends) extended[plans[planName].extends] = true;
    }
    var topPlan = null;
    for (planName in plans) {
      if (!extended[planName] && plans[planName] && plans[planName].extends) topPlan = planName;
    }
    if (!topPlan) return plans;
    var chain = [];
    for (var current = topPlan; current && plans[current] && chain.indexOf(current) < 0; current = plans[current].extends) chain.unshift(current);
    var features = {};
    for (var i = 0; i < chain.length; i++) {
      for (var feature in plans[chain[i]]) {
        if (feature !== 'extends') features[feature] = plans[chain[i]][feature];
      }
    }
    for (planName in plans) {
      var copy = {};
      for (var name in features) copy[name] = features[name];
      plans[planName] = copy;
    }
    return plans;
  };
})();
