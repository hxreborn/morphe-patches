/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class SpeedTestActivity extends HostScreenActivity {

    private static final int CHART_START_PADDING_DP = 5;

    private static final int LEGEND_BOTTOM_PADDING_DP = 8;

    private static final int PROGRESS_DP = 18;

    private static final int PROGRESS_START_DP = 20;

    private static final int SQUARE_DP = 10;

    private static final int SQUARE_CORNER_DP = 3;

    private static final int LEGEND_GAP_DP = 4;

    private static final int BITS_MIN_WIDTH_DP = 85;

    private static final int BYTES_MIN_WIDTH_DP = 80;

    private static final int COMPACT_HEIGHT_DP = 500;

    private static final int COMPACT_LABELS = 2;

    private static final int LABELS = 6;

    private static final double EMPTY_MAX_BPS = 1e7;

    private static final int EMPTY_X_MAX = 150;

    private static final int EMPTY_X_LABELS = 15;

    private static final double FLAT_SPAN_BPS = 16;

    private static final double KILO_SPAN_BPS = 4000;

    private static final double MEGA_SPAN_BPS = 4_000_000;

    private static final double KILO = 1000;

    private static final double MEGA = 1_000_000;

    private static final double RANGE_PADDING = 0.02;

    private static final int X_UNITS_PER_LABEL = 10;

    private static final long MILLIS_PER_X_UNIT = 100;

    private static final int BITS_PER_BYTE = 8;

    private static final float COMPLETE = 1f;

    private TextView toggle;

    private Section download;

    private Section upload;

    private SpeedTest speedTest;

    private WifiManager.WifiLock wifiLock;

    private int run;

    private boolean running;

    @Override
    String screenTitle() {
        return "Internet Speed";
    }

    @Override
    void buildContent() {
        addToolbarIcon(MaterialIcons.HISTORY, "History",
                (view) -> startActivity(new Intent(this, SpeedHistoryActivity.class)));
        this.toggle = addToolbarText("Test", this.palette.startAction, (view) -> {
            if (this.running) {
                stop();
            } else {
                start();
            }
        });
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(sectionHeader("Download"), matchWidth());
        this.download = new Section("Download:", this.palette.green);
        body.addView(this.download.root, weighted());
        body.addView(sectionHeader("Upload"), matchWidth());
        this.upload = new Section("Upload:", this.palette.red);
        body.addView(this.upload.root, weighted());
        replaceBody(body);
    }

    @Override
    protected void onStop() {
        if (this.running) {
            stop();
        }
        super.onStop();
    }

    private static LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    }

    private void start() {
        this.download.reset();
        this.upload.reset();
        this.run++;
        this.running = true;
        setToggle(true);
        this.download.progress.setIndeterminate();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        acquireWifiLock();
        this.speedTest = new SpeedTest(listener(this.run));
        this.speedTest.start();
    }

    private void stop() {
        final SpeedTest current = this.speedTest;
        this.run++;
        if (current != null) {
            current.cancel();
        }
        finishRun();
        this.download.reset();
        this.upload.reset();
    }

    private void finishRun() {
        this.running = false;
        this.speedTest = null;
        setToggle(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        releaseWifiLock();
    }

    private void setToggle(boolean active) {
        this.toggle.setText(active ? "Stop" : "Test");
        this.toggle.setTextColor(active ? this.palette.stopAction : this.palette.startAction);
    }

    private void acquireWifiLock() {
        final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            this.wifiLock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "wifiLock");
            this.wifiLock.setReferenceCounted(false);
            this.wifiLock.acquire();
        } catch (SecurityException ex) {
            this.wifiLock = null;
        }
    }

    private void releaseWifiLock() {
        if (this.wifiLock != null && this.wifiLock.isHeld()) {
            this.wifiLock.release();
        }
        this.wifiLock = null;
    }

    private Section section(SpeedTest.Phase phase) {
        return (phase == SpeedTest.Phase.UPLOAD) ? this.upload : this.download;
    }

    private void post(int token, Runnable action) {
        runOnUiThread(() -> {
            if (token == this.run && this.running) {
                action.run();
            }
        });
    }

    private SpeedTestListener listener(int token) {
        return new SpeedTestListener() {

            @Override
            public void onPhase(SpeedTest.Phase phase) {
                if (phase != SpeedTest.Phase.PREPARING) {
                    post(token, () -> section(phase).progress.setProgress(0));
                }
            }

            @Override
            public void onProgress(SpeedTest.Phase phase, float progress) {
                post(token, () -> section(phase).progress.setProgress(progress));
            }

            @Override
            public void onSample(SpeedTest.Phase phase, long raw, long live, long elapsedMillis) {
                post(token, () -> section(phase).add(elapsedMillis, raw, live));
            }

            @Override
            public void onPhaseResult(SpeedTest.Phase phase, long bytesPerSecond) {
                post(token, () -> section(phase).finish(bytesPerSecond));
            }

            @Override
            public void onFinished(long downloadBytesPerSecond, long uploadBytesPerSecond) {
                post(token, () -> {
                    SpeedHistory.add(SpeedTestActivity.this, downloadBytesPerSecond, uploadBytesPerSecond);
                    finishRun();
                });
            }

            @Override
            public void onFailed(String message) {
                post(token, () -> {
                    stop();
                    Toast.makeText(SpeedTestActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }

        };
    }

    private final class Section {

        final LinearLayout root;

        final ChartView chart;

        final ProgressArc progress;

        private final TextView bits;

        private final TextView bytes;

        private final int plotColor;

        private final List<float[]> points = new ArrayList<>();

        private long firstElapsed;

        Section(String title, int plotColor) {
            final HostPalette palette = SpeedTestActivity.this.palette;
            this.plotColor = plotColor;
            this.root = new LinearLayout(SpeedTestActivity.this);
            this.root.setOrientation(LinearLayout.VERTICAL);
            this.root.setBackgroundColor(palette.background);

            this.chart = new ChartView(SpeedTestActivity.this, palette.grid, palette.rowTitle);
            this.chart.xAxis.axisLineEnabled = false;
            this.chart.yAxis.axisLineEnabled = false;
            this.chart.xAxis.formatter = (value) -> "";
            final LinearLayout.LayoutParams chartParams = weighted();
            chartParams.setMarginStart(dp(CHART_START_PADDING_DP));
            this.root.addView(this.chart, chartParams);

            final LinearLayout legend = new LinearLayout(SpeedTestActivity.this);
            legend.setOrientation(LinearLayout.HORIZONTAL);
            legend.setGravity(Gravity.CENTER_VERTICAL);
            legend.setPadding(0, 0, 0, dp(LEGEND_BOTTOM_PADDING_DP));
            this.progress = new ProgressArc(SpeedTestActivity.this, palette.valueText);
            final LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(PROGRESS_DP),
                    dp(PROGRESS_DP));
            progressParams.setMarginStart(dp(PROGRESS_START_DP));
            legend.addView(this.progress, progressParams);

            final LinearLayout values = new LinearLayout(SpeedTestActivity.this);
            values.setOrientation(LinearLayout.HORIZONTAL);
            values.setGravity(Gravity.CENTER);
            final View square = new View(SpeedTestActivity.this);
            final GradientDrawable squareShape = new GradientDrawable();
            squareShape.setColor(plotColor);
            squareShape.setCornerRadius(dp(SQUARE_CORNER_DP));
            square.setBackground(squareShape);
            values.addView(square, new LinearLayout.LayoutParams(dp(SQUARE_DP), dp(SQUARE_DP)));
            values.addView(text(title, BODY_MEDIUM, palette.rowTitle), gapped());
            this.bits = text("", BODY_MEDIUM, palette.valueText);
            this.bits.setMinWidth(dp(BITS_MIN_WIDTH_DP));
            values.addView(this.bits, gapped());
            this.bytes = text("", BODY_MEDIUM, palette.valueText);
            this.bytes.setMinWidth(dp(BYTES_MIN_WIDTH_DP));
            values.addView(this.bytes, gapped());
            legend.addView(values, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            this.root.addView(legend, matchWidth());
            reset();
        }

        private LinearLayout.LayoutParams gapped() {
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(dp(LEGEND_GAP_DP));
            return params;
        }

        void reset() {
            this.points.clear();
            this.progress.setProgress(0);
            this.chart.clearLimit();
            show(0);
            plot();
        }

        void add(long elapsedMillis, long raw, long live) {
            if (this.points.isEmpty()) {
                this.firstElapsed = elapsedMillis;
            }
            this.points.add(new float[] { (elapsedMillis - this.firstElapsed) / MILLIS_PER_X_UNIT,
                    (float) raw * BITS_PER_BYTE });
            show(live);
            plot();
        }

        void finish(long bytesPerSecond) {
            show(bytesPerSecond);
            this.progress.setProgress(COMPLETE);
            if (!this.points.isEmpty()) {
                this.chart.setLimit((double) bytesPerSecond * BITS_PER_BYTE, SpeedTestActivity.this.palette.yellow);
            }
        }

        private void show(long bytesPerSecond) {
            this.bits.setText(SpeedFormat.bits(bytesPerSecond));
            this.bytes.setText(SpeedFormat.bytes(bytesPerSecond));
        }

        private void plot() {
            final ChartView.Axis x = this.chart.xAxis;
            final ChartView.Axis y = this.chart.yAxis;
            final boolean compact = getResources().getConfiguration().screenHeightDp < COMPACT_HEIGHT_DP;
            y.setLabelCount(compact ? COMPACT_LABELS : LABELS, false);
            if (this.points.isEmpty()) {
                y.setRange(0, EMPTY_MAX_BPS);
                y.formatter = unitFormatter(MEGA);
                x.setRange(0, EMPTY_X_MAX);
                x.setLabelCount(EMPTY_X_LABELS, true);
                this.chart.setLine(this.points, this.plotColor);
                return;
            }
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (float[] point : this.points) {
                min = Math.min(min, point[1]);
                max = Math.max(max, point[1]);
            }
            double span = max - min;
            if (span < FLAT_SPAN_BPS) {
                min = 0;
                span = max;
            }
            final double divisor = (span < MEGA_SPAN_BPS) ? ((span >= KILO_SPAN_BPS) ? KILO : 1) : MEGA;
            final double padding = span * RANGE_PADDING;
            y.setRange(min - padding, max + padding);
            y.formatter = unitFormatter(divisor);
            final float last = this.points.get(this.points.size() - 1)[0];
            x.setRange(0, last);
            x.setLabelCount((int) (last / X_UNITS_PER_LABEL), false);
            this.chart.setLine(this.points, this.plotColor);
        }

        private ValueFormatter unitFormatter(double divisor) {
            final String unit = (divisor < KILO) ? "bps" : ((divisor < MEGA) ? "kbps" : "Mbps");
            return (value) -> (long) (value / divisor) + " " + unit;
        }

    }

}
