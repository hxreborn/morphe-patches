(function () {
  if (window.hxChartLayouts) return;
  window.hxChartLayouts = true;
  var originalParse = JSON.parse;
  JSON.parse = function () {
    var plans = originalParse.apply(this, arguments);
    if (!plans || !plans.free || !plans.free.MULTIPLE_CHARTS) return plans;
    var largestLimit = 0;
    var planName;
    for (planName in plans) {
      var multipleCharts = plans[planName] && plans[planName].MULTIPLE_CHARTS;
      if (multipleCharts && multipleCharts.limit > largestLimit) largestLimit = multipleCharts.limit;
    }
    for (planName in plans) {
      if (plans[planName] && plans[planName].MULTIPLE_CHARTS) plans[planName].MULTIPLE_CHARTS.limit = largestLimit;
    }
    return plans;
  };
})();
