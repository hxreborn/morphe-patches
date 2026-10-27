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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.Q)
abstract class BrowserScreenActivity extends HostScreenActivity {

    static final String EXTRA_IP = "ip";

    static final String EXTRA_ARGUMENT = "data";

    static final int NO_ARGUMENT = -1;

    private static final int INSET_DP = 16;

    private static final int COLUMN_VERTICAL_DP = 6;

    private static final int NAME_GAP_DP = 2;

    private static final int LABEL_GAP_DP = 4;

    private static final TextStyle DETAIL = new TextStyle(12, 20, 0.2f, TextStyle.NORMAL);

    private static final String MULTICAST_LOCK_TAG = "multicastLock";

    private static final int REFRESH_DELAY_MS = 150;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable rebuild = this::rebuildNow;

    private WifiManager.MulticastLock multicastLock;

    private InetAddress bindAddress;

    private Network bindNetwork;

    private NetworkInterface bindInterface;

    private boolean unavailable;

    boolean browsing() {
        return false;
    }

    void startBrowser(InetAddress address, Network network, NetworkInterface networkInterface) throws IOException {
    }

    void stopBrowser() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!browsing()) {
            return;
        }
        final WifiManager wifi = getApplicationContext().getSystemService(WifiManager.class);
        if (wifi != null) {
            this.multicastLock = wifi.createMulticastLock(MULTICAST_LOCK_TAG);
            this.multicastLock.setReferenceCounted(false);
            this.multicastLock.acquire();
        }
        boolean started = false;
        if (resolveNetwork()) {
            try {
                startBrowser(this.bindAddress, this.bindNetwork, this.bindInterface);
                started = true;
            } catch (IOException ex) {
                started = false;
            }
        }
        this.unavailable = !started;
        rebuildNow();
    }

    @Override
    protected void onStop() {
        if (browsing()) {
            stopBrowser();
            this.handler.removeCallbacks(this.rebuild);
            if (this.multicastLock != null && this.multicastLock.isHeld()) {
                this.multicastLock.release();
            }
        }
        super.onStop();
    }

    final void refresh() {
        if (!this.handler.hasCallbacks(this.rebuild)) {
            this.handler.postDelayed(this.rebuild, REFRESH_DELAY_MS);
        }
    }

    final boolean hasArgument() {
        return getIntent().getIntExtra(EXTRA_ARGUMENT, NO_ARGUMENT) != NO_ARGUMENT;
    }

    final Object argument() {
        return ScreenArguments.get(getIntent().getIntExtra(EXTRA_ARGUMENT, NO_ARGUMENT));
    }

    final String ipFilter() {
        return getIntent().getStringExtra(EXTRA_IP);
    }

    final boolean showUnavailable() {
        if (this.unavailable) {
            section("Not connected to Wi-Fi");
        }
        return this.unavailable;
    }

    static boolean matches(String ipFilter, InetAddress address) {
        return ipFilter == null || (address != null && ipFilter.equals(address.getHostAddress()));
    }

    final LinearLayout infoColumn() {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(this.palette.background);
        column.setPaddingRelative(dp(INSET_DP), dp(COLUMN_VERTICAL_DP), dp(INSET_DP), dp(COLUMN_VERTICAL_DP));
        return column;
    }

    final TextView nameLine(LinearLayout column, String name) {
        final TextView view = text((name != null) ? name : "", BODY_MEDIUM, this.palette.valueText);
        view.setTextIsSelectable(true);
        final LinearLayout.LayoutParams params = matchWidth();
        params.bottomMargin = dp(NAME_GAP_DP);
        column.addView(view, params);
        return view;
    }

    final void propertyLines(LinearLayout column, Map<String, String> properties) {
        if (properties.isEmpty()) {
            return;
        }
        final SpannableStringBuilder lines = new SpannableStringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (lines.length() > 0) {
                lines.append('\n');
            }
            appendColored(lines, entry.getKey(), this.palette.rowSubtitle);
            appendColored(lines, " = ", this.palette.rowSubtitle);
            appendColored(lines, entry.getValue(), this.palette.valueText);
        }
        final TextView view = text("", DETAIL, this.palette.valueText);
        view.setText(lines);
        view.setTextIsSelectable(true);
        final LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(NAME_GAP_DP);
        column.addView(view, params);
    }

    final void labelLine(LinearLayout column, String label, String value) {
        final LinearLayout line = new LinearLayout(this);
        labelValue(line, label, value, true);
        column.addView(line, matchWidth());
    }

    final void labelPairLine(LinearLayout column, String label, String value, String endLabel, String endValue) {
        final LinearLayout line = new LinearLayout(this);
        labelValue(line, label, value, true);
        final LinearLayout end = new LinearLayout(this);
        labelValue(end, endLabel, endValue, false);
        line.addView(end, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(line, matchWidth());
    }

    private void labelValue(LinearLayout line, String label, String value, boolean weighted) {
        line.addView(text(label, DETAIL, this.palette.rowTitle));
        final TextView valueView = text(value, DETAIL, this.palette.valueText);
        valueView.setTextIsSelectable(true);
        final LinearLayout.LayoutParams params = weighted
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(LABEL_GAP_DP));
        line.addView(valueView, params);
    }

    static void appendColored(SpannableStringBuilder builder, String value, int color) {
        final int start = builder.length();
        builder.append(value);
        builder.setSpan(new ForegroundColorSpan(color), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void rebuildNow() {
        this.list.removeAllViews();
        buildContent();
    }

    @SuppressLint("MissingPermission")
    private boolean resolveNetwork() {
        final ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        Network chosen = null;
        for (Network network : connectivity.getAllNetworks()) {
            final NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                chosen = network;
                break;
            }
        }
        if (chosen == null) {
            chosen = connectivity.getActiveNetwork();
        }
        final LinkProperties properties = (chosen != null) ? connectivity.getLinkProperties(chosen) : null;
        if (properties == null) {
            return false;
        }
        this.bindNetwork = chosen;
        this.bindAddress = null;
        for (LinkAddress linkAddress : properties.getLinkAddresses()) {
            if (linkAddress.getAddress() instanceof Inet4Address && !linkAddress.getAddress().isLoopbackAddress()) {
                this.bindAddress = linkAddress.getAddress();
                break;
            }
        }
        try {
            this.bindInterface = (properties.getInterfaceName() != null)
                    ? NetworkInterface.getByName(properties.getInterfaceName()) : null;
        } catch (SocketException ex) {
            this.bindInterface = null;
        }
        return this.bindAddress != null;
    }

}
