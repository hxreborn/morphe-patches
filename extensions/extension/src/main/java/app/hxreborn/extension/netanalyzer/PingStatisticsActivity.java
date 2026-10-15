/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class PingStatisticsActivity extends HostScreenActivity {

    static final TextStyle LEGEND = new TextStyle(14, 24, 0.5f, TextStyle.NORMAL);

    private static final long REFRESH_MILLIS = 1000;

    private static final double PERCENT = 100.0;

    private static final double AXIS_HEADROOM = 1.05;

    private static final double EMPTY_AXIS_MAX_MS = 100;

    private static final double BAR_HALF_SLOT = 0.5;

    private static final int Y_LABELS = 6;

    private static final int X_LABELS = 6;

    private static final int MIN_BARS_FOR_EXTREMES = 2;

    private static final int LEGEND_ROW_DP = 25;

    private static final int LEGEND_START_DP = 40;

    private static final int LEGEND_VERTICAL_DP = 5;

    private static final int SQUARE_DP = 10;

    private static final int SQUARE_CORNER_DP = 3;

    private static final int SQUARE_TOP_DP = 1;

    private static final int GAP_DP = 4;

    private static final int LEFT_LABEL_DP = 35;

    private static final int LEFT_VALUE_DP = 75;

    private static final int RIGHT_LABEL_DP = 55;

    private static final int RIGHT_VALUE_DP = 95;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refresh = this::refresh;

    private ChartView chart;

    private TextView toggle;

    private TextView minimum;

    private TextView median;

    private TextView maximum;

    private TextView lost;

    private boolean running;

    @Override
    String screenTitle() {
        return "Ping Statistics";
    }

    @Override
    void buildContent() {
        this.toggle = addToolbarText("Start", this.palette.startAction, (view) -> {
            if (this.running) {
                HostTools.stopPing();
            } else {
                HostTools.restartPing();
            }
            refresh();
        });
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        this.chart = new ChartView(this, this.palette.grid, this.palette.rowTitle);
        this.chart.xAxis.gridEnabled = false;
        this.chart.xAxis.granularity = 1;
        this.chart.xAxis.formatter = (value) -> String.valueOf((int) value + 1);
        this.chart.xAxis.setLabelCount(X_LABELS, false);
        this.chart.yAxis.formatter = (value) -> (int) value + " ms";
        this.chart.yAxis.setLabelCount(Y_LABELS, false);
        body.addView(this.chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final LinearLayout top = legendRow();
        top.setPadding(dp(LEGEND_START_DP), dp(LEGEND_VERTICAL_DP), 0, 0);
        this.minimum = legendPair(top, this.palette.green, "Min:", LEFT_LABEL_DP, LEFT_VALUE_DP);
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
        this.median = legendPair(top, this.palette.yellow, "Median:", RIGHT_LABEL_DP, RIGHT_VALUE_DP);
        body.addView(top, matchWidth());

        final LinearLayout bottom = legendRow();
        bottom.setPadding(dp(LEGEND_START_DP), 0, 0, dp(LEGEND_VERTICAL_DP));
        this.maximum = legendPair(bottom, this.palette.red, "Max:", LEFT_LABEL_DP, LEFT_VALUE_DP);
        bottom.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
        this.lost = legendPair(bottom, this.palette.grey, "Lost:", RIGHT_LABEL_DP, RIGHT_VALUE_DP);
        body.addView(bottom, matchWidth());
        replaceBody(body);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.handler.removeCallbacks(this.refresh);
    }

    private LinearLayout legendRow() {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(LEGEND_ROW_DP));
        return row;
    }

    private TextView legendPair(LinearLayout row, int color, String label, int labelDp, int valueDp) {
        final View square = new View(this);
        final GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(SQUARE_CORNER_DP));
        square.setBackground(shape);
        final LinearLayout.LayoutParams squareParams = new LinearLayout.LayoutParams(dp(SQUARE_DP), dp(SQUARE_DP));
        squareParams.topMargin = dp(SQUARE_TOP_DP);
        squareParams.setMarginEnd(dp(GAP_DP));
        row.addView(square, squareParams);
        final TextView name = text(label, LEGEND, this.palette.rowTitle);
        name.setSingleLine(true);
        row.addView(name, new LinearLayout.LayoutParams(dp(labelDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView value = text("", LEGEND, this.palette.valueText);
        value.setSingleLine(true);
        value.setPaddingRelative(0, value.getPaddingTop(), dp(GAP_DP), value.getPaddingBottom());
        row.addView(value, new LinearLayout.LayoutParams(dp(valueDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        return value;
    }

    private void refresh() {
        final HostTools.Snapshot snapshot = HostTools.ping();
        final PingStatistics stats = PingStatistics.of(snapshot.probes, snapshot.active);
        this.running = snapshot.active;
        showRunState(this.toggle, this.running, HostTools.canRestartPing());
        plot(stats);
        this.minimum.setText(milliseconds(stats.min));
        this.median.setText(milliseconds(stats.median));
        this.maximum.setText(milliseconds(stats.max));
        this.lost.setText(String.format(Locale.US, "%d pkt (%.0f %%)", stats.lost, stats.lostFraction * PERCENT));
        this.handler.removeCallbacks(this.refresh);
        this.handler.postDelayed(this.refresh, REFRESH_MILLIS);
    }

    private void plot(PingStatistics stats) {
        final int count = stats.bars.size();
        this.chart.yAxis.setRange(0, (count == 0) ? EMPTY_AXIS_MAX_MS : stats.max * AXIS_HEADROOM);
        this.chart.xAxis.setRange(-BAR_HALF_SLOT, Math.max(count, 1) - BAR_HALF_SLOT);
        final List<ChartView.Bar> bars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bars.add(new ChartView.Bar(i, new double[] { stats.bars.get(i) }, new int[] { barColor(stats, i) }));
        }
        this.chart.setBars(bars);
        if (count >= MIN_BARS_FOR_EXTREMES) {
            this.chart.setLimit(stats.median, this.palette.yellow);
        } else {
            this.chart.clearLimit();
        }
    }

    private int barColor(PingStatistics stats, int index) {
        if (stats.received <= 1) {
            return this.palette.blue;
        }
        if (index == stats.maxIndex) {
            return this.palette.red;
        }
        return (index == stats.minIndex) ? this.palette.green : this.palette.blue;
    }

    private static String milliseconds(double value) {
        return String.format(Locale.US, "%.1f ms", value);
    }

}
