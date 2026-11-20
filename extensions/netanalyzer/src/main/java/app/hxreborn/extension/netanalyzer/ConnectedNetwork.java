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

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

final class ConnectedNetwork {

    private static final String WIFI = "Wi-Fi";

    private static final String QUOTE = "\"";

    private static final String UNKNOWN_SSID = "<unknown ssid>";

    private ConnectedNetwork() {
    }

    @SuppressLint("MissingPermission")
    static String transportLabel(Context context) {
        try {
            final ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network network = connectivity.getActiveNetwork();
            final NetworkCapabilities capabilities = (network != null) ? connectivity.getNetworkCapabilities(network)
                    : null;
            if (capabilities == null) {
                return "";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return WIFI;
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mobile";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            }
        } catch (SecurityException ex) {
            return "";
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    static String ssid(Context context) {
        if (!WIFI.equals(transportLabel(context))) {
            return "";
        }
        try {
            final WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connection = wifi.getConnectionInfo();
            String ssid = (connection != null) ? connection.getSSID() : null;
            if (ssid == null || UNKNOWN_SSID.equals(ssid)) {
                return "";
            }
            if (ssid.length() > QUOTE.length() && ssid.startsWith(QUOTE) && ssid.endsWith(QUOTE)) {
                ssid = ssid.substring(QUOTE.length(), ssid.length() - QUOTE.length());
            }
            return ssid;
        } catch (SecurityException ex) {
            return "";
        }
    }

}
