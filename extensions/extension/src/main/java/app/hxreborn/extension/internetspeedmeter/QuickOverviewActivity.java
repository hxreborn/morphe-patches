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
package app.hxreborn.extension.internetspeedmeter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

public final class QuickOverviewActivity extends Activity {

    private static final long SAMPLE_INTERVAL_MS = 1000;

    private static final int UPLOAD_COLOR = Color.rgb(102, 102, 102);

    private static final int WIDTH_DP = 340;

    private static final int GRAPH_HEIGHT_DP = 160;

    private static final int USAGE_HEIGHT_DP = 220;

    private static final int PADDING_DP = 16;

    private static final int ROW_PADDING_DP = 6;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable sampler = this::sample;

    private SharedPreferences preferences;

    private SpeedGraphView graph;

    private TextView sessionTime;

    private TextView sessionUsage;

    private TextView currentSpeed;

    private LinearLayout usageRows;

    private TextView mobileTab;

    private TextView wifiTab;

    private int headerColor;

    private boolean bits;

    private long lastReceived = -1;

    private long lastSent = -1;

    private int usageNetwork;

    private int usageRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.preferences = AppResources.preferences(this);
        this.headerColor = AppResources.headerColor(this.preferences);
        this.bits = this.preferences.getBoolean("Use_bits", false);
        this.usageNetwork = isMobileActive() ? ConnectivityManager.TYPE_MOBILE : ConnectivityManager.TYPE_WIFI;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setMinimumWidth(dp(WIDTH_DP));
        root.addView(header());

        this.currentSpeed = text(14, Color.DKGRAY);
        this.currentSpeed.setPadding(dp(PADDING_DP), dp(ROW_PADDING_DP), dp(PADDING_DP), 0);
        root.addView(this.currentSpeed);

        this.graph = new SpeedGraphView(this, this.headerColor, UPLOAD_COLOR, this.bits);
        root.addView(this.graph,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(GRAPH_HEIGHT_DP)));

        root.addView(tabs());
        ScrollView usageScroll = new ScrollView(this);
        this.usageRows = new LinearLayout(this);
        this.usageRows.setOrientation(LinearLayout.VERTICAL);
        this.usageRows.setPadding(dp(PADDING_DP), dp(ROW_PADDING_DP), dp(PADDING_DP), dp(PADDING_DP));
        usageScroll.addView(this.usageRows);
        root.addView(usageScroll,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(USAGE_HEIGHT_DP)));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.lastReceived = -1;
        this.lastSent = -1;
        showUsage();
        this.handler.post(this.sampler);
    }

    @Override
    protected void onPause() {
        this.handler.removeCallbacks(this.sampler);
        super.onPause();
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setBackgroundColor(this.headerColor);
        header.setPadding(dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP));
        this.sessionTime = text(18, Color.WHITE);
        this.sessionUsage = text(18, Color.WHITE);
        header.addView(column(AppResources.string(this, "session_time"), this.sessionTime), weighted());
        header.addView(column(AppResources.string(this, "session_usage"), this.sessionUsage), weighted());
        return header;
    }

    private View column(String label, TextView value) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(text(13, Color.WHITE, label));
        value.setTypeface(Typeface.DEFAULT_BOLD);
        column.addView(value);
        return column;
    }

    private View tabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(PADDING_DP), dp(ROW_PADDING_DP), dp(PADDING_DP), 0);
        this.mobileTab = tab(AppResources.string(this, "Mobile"), ConnectivityManager.TYPE_MOBILE);
        this.wifiTab = tab(AppResources.string(this, "Wifi"), ConnectivityManager.TYPE_WIFI);
        tabs.addView(this.mobileTab, weighted());
        tabs.addView(this.wifiTab, weighted());
        return tabs;
    }

    private TextView tab(String label, int network) {
        TextView tab = text(15, Color.DKGRAY, label);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(0, dp(ROW_PADDING_DP), 0, dp(ROW_PADDING_DP));
        tab.setOnClickListener((view) -> {
            this.usageNetwork = network;
            showUsage();
        });
        return tab;
    }

    private void sample() {
        long received = TrafficStats.getTotalRxBytes();
        long sent = TrafficStats.getTotalTxBytes();
        if (this.lastReceived >= 0) {
            long download = Math.max(0, received - this.lastReceived);
            long upload = Math.max(0, sent - this.lastSent);
            this.graph.add(download, upload);
            this.currentSpeed.setText(
                    "↓ " + SpeedFormat.speed(download, this.bits) + "    ↑ " + SpeedFormat.speed(upload, this.bits));
        }
        this.lastReceived = received;
        this.lastSent = sent;

        long sessionStart = this.preferences.getLong("session_start_time", 0);
        this.sessionTime
            .setText((sessionStart > 0) ? SpeedFormat.duration(System.currentTimeMillis() - sessionStart) : "-");
        float reference = this.preferences.getFloat("session_usage_ref", 0f);
        this.sessionUsage.setText(SpeedFormat.size((long) Math.max(0f, sessionCounter(this.preferences) - reference)));

        this.handler.postDelayed(this.sampler, SAMPLE_INTERVAL_MS);
    }

    private static float sessionCounter(SharedPreferences preferences) {
        return preferences.getFloat("session_usage_ref", 0f);
    }

    private void showUsage() {
        boolean mobile = this.usageNetwork == ConnectivityManager.TYPE_MOBILE;
        styleTab(this.mobileTab, mobile);
        styleTab(this.wifiTab, !mobile);
        this.usageRows.removeAllViews();

        if (!hasUsageAccess()) {
            TextView request = text(14, this.headerColor, AppResources.string(this, "app_usage_permission_request"));
            request.setOnClickListener((view) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            this.usageRows.addView(request);
            return;
        }

        int request = ++this.usageRequest;
        int network = this.usageNetwork;
        new Thread(() -> {
            List<Map.Entry<String, Long>> usage = labelledUsage(network);
            this.handler.post(() -> showUsageRows(request, mobile, usage));
        }).start();
    }

    private void showUsageRows(int request, boolean mobile, List<Map.Entry<String, Long>> usage) {
        if (request != this.usageRequest || isFinishing()) {
            return;
        }
        if (usage.isEmpty()) {
            this.usageRows
                .addView(text(14, Color.GRAY, AppResources.string(this, mobile ? "no_data_mobile" : "no_data_wifi")));
            return;
        }
        for (Map.Entry<String, Long> entry : usage) {
            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, dp(ROW_PADDING_DP), 0, dp(ROW_PADDING_DP));
            row.addView(text(14, Color.BLACK, entry.getKey()), weighted());
            row.addView(text(14, Color.DKGRAY, SpeedFormat.size(entry.getValue())));
            this.usageRows.addView(row);
        }
    }

    private List<Map.Entry<String, Long>> labelledUsage(int network) {
        List<Map.Entry<String, Long>> labelled = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : todayUsage(network)) {
            labelled.add(new AbstractMap.SimpleEntry<>(appName(entry.getKey()), entry.getValue()));
        }
        return labelled;
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? Color.WHITE : Color.DKGRAY);
        tab.setBackgroundColor(selected ? this.headerColor : Color.TRANSPARENT);
    }

    private List<Map.Entry<Integer, Long>> todayUsage(int network) {
        Map<Integer, Long> bytesByUid = new HashMap<>();
        NetworkStatsManager statsManager = getSystemService(NetworkStatsManager.class);
        NetworkStats stats;
        try {
            stats = statsManager.querySummary(network, null, dayStart(), System.currentTimeMillis());
        } catch (RemoteException | RuntimeException ex) {
            return new ArrayList<>();
        }
        if (stats == null) {
            return new ArrayList<>();
        }
        try {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket);
                long bytes = bucket.getRxBytes() + bucket.getTxBytes();
                Long previous = bytesByUid.get(bucket.getUid());
                bytesByUid.put(bucket.getUid(), (previous != null) ? previous + bytes : bytes);
            }
        } finally {
            stats.close();
        }
        List<Map.Entry<Integer, Long>> usage = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : bytesByUid.entrySet()) {
            if (entry.getValue() > 0) {
                usage.add(entry);
            }
        }
        Collections.sort(usage, (first, second) -> Long.compare(second.getValue(), first.getValue()));
        return usage;
    }

    private long dayStart() {
        int startingHour = this.preferences.getInt(PremiumSettings.STARTING_HOUR, 0);
        Calendar start = Calendar.getInstance();
        if (start.get(Calendar.HOUR_OF_DAY) < startingHour) {
            start.add(Calendar.DAY_OF_MONTH, -1);
        }
        start.set(Calendar.HOUR_OF_DAY, startingHour);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTimeInMillis();
    }

    private String appName(int uid) {
        if (uid == NetworkStats.Bucket.UID_REMOVED) {
            return "Removed apps";
        }
        if (uid == NetworkStats.Bucket.UID_TETHERING) {
            return "Tethering";
        }
        if (uid == Process.SYSTEM_UID) {
            return "Android system";
        }
        PackageManager packageManager = getPackageManager();
        String[] packages = packageManager.getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return "UID " + uid;
        }
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packages[0], 0);
            return packageManager.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException ex) {
            return packages[0];
        }
    }

    private boolean hasUsageAccess() {
        AppOpsManager appOps = getSystemService(AppOpsManager.class);
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    @SuppressLint("MissingPermission")
    private boolean isMobileActive() {
        ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private TextView text(int sizeSp, int color) {
        return text(sizeSp, color, "");
    }

    private TextView text(int sizeSp, int color, String value) {
        TextView text = new TextView(this);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        text.setTextColor(color);
        text.setText(value);
        return text;
    }

    private static LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int value) {
        return Dimensions.dp(this, value);
    }

}
