/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class ChannelUsageActivity extends HostScreenActivity {

    private static final TextStyle CAPTION = new TextStyle(11, 16, 0.5f, TextStyle.MEDIUM);

    private static final String SIGNAL_PREFERENCES = "WifiSignal";

    private static final String BAND_KEY = "lastUsedWifiSignalBand";

    private static final double BAR_CENTER_OFFSET = 0.5;

    private static final int Y_LINES = 11;

    private static final int CAPTION_HORIZONTAL_DP = 4;

    private static final int CAPTION_TOP_DP = 3;

    private static final int CAPTION_BOTTOM_DP = 6;

    private final ChannelUsage.Colors colors = new ChannelUsage.Colors();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }

    };

    private WifiManager wifi;

    private SharedPreferences preferences;

    private ChannelUsage.Band band;

    private ChartView chart;

    @Override
    String screenTitle() {
        return "Wi-Fi Signal";
    }

    @Override
    void buildContent() {
        this.wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.preferences = getSharedPreferences(SIGNAL_PREFERENCES, MODE_PRIVATE);
        final ChannelUsage.Band[] bands = ChannelUsage.Band.values();
        this.band = bands[Math.max(0, Math.min(bands.length - 1, this.preferences.getInt(BAND_KEY, 0)))];
        addToolbarIcon(MaterialIcons.SWITCH_FREQUENCY, "Switch Frequency", (view) -> switchBand());

        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        this.chart = new ChartView(this, this.palette.grid, this.palette.rowTitle);
        this.chart.yAxis.setRange(0, ChannelUsage.Y_AXIS_MAX);
        this.chart.yAxis.setLabelCount(Y_LINES, true);
        this.chart.yAxis.formatter = (value) -> "";
        body.addView(this.chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        final TextView caption = text("Channel", CAPTION, this.palette.rowTitle);
        caption.setGravity(Gravity.CENTER_HORIZONTAL);
        caption.setPaddingRelative(dp(CAPTION_HORIZONTAL_DP), caption.getPaddingTop() + dp(CAPTION_TOP_DP),
                dp(CAPTION_HORIZONTAL_DP), caption.getPaddingBottom() + dp(CAPTION_BOTTOM_DP));
        body.addView(caption, matchWidth());
        replaceBody(body);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(this.receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(this.receiver, filter);
        }
        refresh();
        requestScan();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(this.receiver);
        super.onStop();
    }

    private void switchBand() {
        final ChannelUsage.Band[] bands = ChannelUsage.Band.values();
        ChannelUsage.Band next = bands[(this.band.ordinal() + 1) % bands.length];
        if (next == ChannelUsage.Band.GHZ_6 && !this.wifi.is6GHzBandSupported()) {
            next = bands[0];
        }
        this.band = next;
        this.preferences.edit().putInt(BAND_KEY, next.ordinal()).apply();
        refresh();
    }

    @SuppressLint("MissingPermission")
    private void requestScan() {
        try {
            this.wifi.startScan();
        } catch (SecurityException ex) {
            return;
        }
    }

    @SuppressLint("MissingPermission")
    private void refresh() {
        final List<ChannelUsage.Network> scanned = new ArrayList<>();
        try {
            for (ScanResult result : this.wifi.getScanResults()) {
                scanned.add(ChannelUsage.Network.of(result.SSID, result.BSSID, result.frequency, result.level,
                        result.channelWidth, result.centerFreq0, result.centerFreq1));
            }
        } catch (SecurityException ex) {
            scanned.clear();
        }
        final ChannelUsage usage = new ChannelUsage(this.band, scanned);
        this.chart.xAxis.setRange(this.band.axisMin, this.band.axisMax);
        this.chart.xAxis.setLabelCount(this.band.labelCount, false);
        final ChannelUsage.Band shown = this.band;
        this.chart.xAxis.formatter = (value) -> shown.tickLabel((int) Math.round(value));
        final List<ChartView.Bar> bars = new ArrayList<>();
        for (Map.Entry<Integer, List<ChannelUsage.Network>> stack : usage.stacks().entrySet()) {
            final int bucket = stack.getKey();
            if (bucket < this.band.firstBucket || bucket >= this.band.endBucket) {
                continue;
            }
            final List<ChannelUsage.Network> networks = stack.getValue();
            final double[] heights = new double[networks.size()];
            final int[] segmentColors = new int[networks.size()];
            for (int i = 0; i < networks.size(); i++) {
                heights[i] = usage.segmentHeight(networks.get(i));
                segmentColors[i] = this.colors.of(this.band, networks.get(i));
            }
            bars.add(new ChartView.Bar(bucket + BAR_CENTER_OFFSET, heights, segmentColors));
        }
        this.chart.setBars(bars);
    }

}
