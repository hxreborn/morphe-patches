/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class ExportMenu {

    private static final String TAG = "hxNetAnalyzer";

    private static final Map<String, Object> EXPORT_MENUS = new HashMap<>();

    private static final String EXPORT_PING = "ping";

    private static final String EXPORT_TRACEROUTE = "traceroute";

    private static final String EXPORT_LAN = "lan";

    private static final String EXPORT_PORT_SCAN = "portscan";

    private static final String EXPORT_DNS = "dns";

    private static final String EXPORT_WIFI = "wifi";

    private static final String EXPORT_INFORMATION = "information";

    private static final String EXPORT_WHOIS = "whois";

    private static final int TOOLBAR_HEIGHT_DP = 64;

    private static final int ICON_BUTTON_DP = 48;

    private static final int ICON_BUTTON_END_DP = 4;

    private static final String ANCHOR_TAG = "hxExportMenuAnchor";

    private ExportMenu() {
    }

    public static synchronized Object onExportMenu(Class<?> onClickType, String screen) {
        Object proxy = EXPORT_MENUS.get(screen);
        if (proxy == null) {
            proxy = HostCallbacks.create(onClickType, () -> {
                final Activity activity = HostActivity.current();
                final ExportSource source = exportSource(screen);
                if (activity != null && source != null) {
                    showFromHostToolbar(activity, source);
                }
            });
            EXPORT_MENUS.put(screen, proxy);
        }
        return proxy;
    }

    private static ExportSource exportSource(String screen) {
        switch (screen) {
            case EXPORT_PING:
                return ProbeExport.ping();
            case EXPORT_TRACEROUTE:
                return ProbeExport.traceroute();
            case EXPORT_LAN:
                final LanScan scan = LiveLanScan.currentScan();
                return (scan != null) ? new LanExport(scan) : null;
            case EXPORT_PORT_SCAN:
                return new PortScanExport();
            case EXPORT_DNS:
                return new DnsExport();
            case EXPORT_WIFI:
                return new WifiExport();
            case EXPORT_INFORMATION:
                return new InformationExport();
            case EXPORT_WHOIS:
                return new WhoisExport();
            default:
                Log.e(TAG, "Unknown export " + screen);
                return null;
        }
    }

    static void show(Activity activity, View anchor, ExportSource source) {
        final Runnable plainText = () -> ExportDelivery.shareText(activity, source.subject(activity),
                source.text(activity));
        if (!(source instanceof TableExport)) {
            new DropdownMenu(activity).add("Export", MaterialIcons.SHARE, plainText).show(anchor);
            return;
        }
        final TableExport table = (TableExport) source;
        final DropdownMenu formats = new DropdownMenu(activity).add("Plain Text", MaterialIcons.TEXT_SNIPPET, plainText)
            .add("CSV File", MaterialIcons.TABLE_CHART, () -> ExportDelivery.shareCsv(activity, table.csv(activity)))
            .add("Configure…", MaterialIcons.SETTINGS, () -> ExportConfiguration.show(activity));
        new DropdownMenu(activity).addSubmenu("Export", MaterialIcons.SHARE, () -> formats.show(anchor)).show(anchor);
    }

    private static void showFromHostToolbar(Activity activity, ExportSource source) {
        final ViewGroup content = activity.findViewById(android.R.id.content);
        View anchor = content.findViewWithTag(ANCHOR_TAG);
        if (anchor == null) {
            anchor = new View(activity);
            anchor.setTag(ANCHOR_TAG);
            final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(activity, ICON_BUTTON_DP),
                    dp(activity, ICON_BUTTON_DP), Gravity.TOP | Gravity.END);
            final WindowInsets insets = content.getRootWindowInsets();
            final int statusBar = (insets != null) ? insets.getInsets(WindowInsets.Type.statusBars()).top : 0;
            params.topMargin = statusBar + (dp(activity, TOOLBAR_HEIGHT_DP) - dp(activity, ICON_BUTTON_DP)) / 2;
            params.setMarginEnd(dp(activity, ICON_BUTTON_END_DP));
            content.addView(anchor, params);
        }
        final View placedAnchor = anchor;
        placedAnchor.post(() -> show(activity, placedAnchor, source));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                activity.getResources().getDisplayMetrics()));
    }

}
