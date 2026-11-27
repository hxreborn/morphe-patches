(function () {
  if (window.hxPlanLimits) return;
  window.hxPlanLimits = true;
  var CHART_HOST = 'widgetdata.tradingview.com';
  var REPLAY_HOST = 'history-data.tradingview.com';
  var REPLAY_CREATE_PREFIX = '{"m":"replay_create_session"';
  var REPLAY_DELETE_PREFIX = '{"m":"replay_delete_session"';
  var SERVER_LIMITED_FEATURES = [
    'BAR_REPLAY_INTRADAY', 'BAR_REPLAY_1S', 'BAR_REPLAY_1T',
    'SERVER_SIDE_ALERTS', 'NEWS_ALERTS', 'ALERTS_AI_CREATION', 'ALERTS_MULTICONDITIONS', 'ALERTS_ON_SECONDS',
    'ALERTS_NO_EXPIRATION', 'ALERTS_WEBHOOK', 'ALERTS_ON_EARNINGS', 'ALERTS_ON_FUNDAMENTAL_METRICS',
    'MULTIPLE_WATCHLISTS', 'IMPORT_WATCHLISTS', 'WATCHLIST_SYMBOLS',
    'MULTICOLOR_FLAGGED_SYMBOLS', 'MULTIFLAGGED_SYMBOLS_LISTS'
  ];
  Object.defineProperty(window, 'WEBSOCKET_HOST', {
    configurable: true,
    get: function () { return CHART_HOST; },
    set: function () {}
  });
  var connection;
  Object.defineProperty(window, 'WSBackendConnection', {
    configurable: true,
    get: function () { return connection; },
    set: function (value) {
      connection = value;
      if (connection && connection.send) routeReplayToReplayHost(connection);
    }
  });
  function routeReplayToReplayHost(backendConnection) {
    var originalSend = backendConnection.send;
    function reconnectTo(host) {
      if (backendConnection._suggestedHost === host) return;
      backendConnection._suggestedHost = host;
      backendConnection.disconnect();
      backendConnection.connect();
    }
    backendConnection.send = function (message) {
      if (typeof message === 'string') {
        if (message.indexOf(REPLAY_CREATE_PREFIX) === 0 && backendConnection._suggestedHost === CHART_HOST) {
          reconnectTo(REPLAY_HOST);
          return false;
        }
        if (message.indexOf(REPLAY_DELETE_PREFIX) === 0 && backendConnection._suggestedHost === REPLAY_HOST) {
          var sent = originalSend.apply(this, arguments);
          setTimeout(function () { reconnectTo(CHART_HOST); }, 0);
          return sent;
        }
      }
      return originalSend.apply(this, arguments);
    };
  }
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
    function resolvedFeatures(name) {
      var chain = [];
      for (var current = name; current && plans[current] && chain.indexOf(current) < 0; current = plans[current].extends) chain.unshift(current);
      var features = {};
      for (var i = 0; i < chain.length; i++) {
        for (var feature in plans[chain[i]]) {
          if (feature !== 'extends') features[feature] = plans[chain[i]][feature];
        }
      }
      return features;
    }
    var topFeatures = resolvedFeatures(topPlan);
    var unlocked = {};
    for (planName in plans) {
      var planFeatures = resolvedFeatures(planName);
      var copy = {};
      for (var name in topFeatures) copy[name] = topFeatures[name];
      for (var k = 0; k < SERVER_LIMITED_FEATURES.length; k++) {
        var limited = SERVER_LIMITED_FEATURES[k];
        if (limited in planFeatures) copy[limited] = planFeatures[limited];
        else delete copy[limited];
      }
      unlocked[planName] = copy;
    }
    for (planName in unlocked) plans[planName] = unlocked[planName];
    return plans;
  };
})();
