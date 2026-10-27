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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@SuppressWarnings("unused")
public final class InterfaceCounters {

    private static final String[][] WIFI_INTERFACES = { { "wlan0" }, { "eth0" }, { "tiwlan0" }, { "athwlan0" } };

    private static final String[][] MOBILE_INTERFACES = { { "rmnet0", "rmnet1" }, { "rmnet_usb0" },
            { "ccmni0", "ccmni1" }, { "ppp0" }, { "rmnet0", "rmnet1" }, { "rmnet_usb1" }, { "ccmni0", "ccmni1" },
            { "rmnet_data0" } };

    private InterfaceCounters() {

    }

    public static long getIfaceRxBytes(String iface) {
        return (iface != null) ? read(iface, "rx_bytes") : -1;
    }

    public static long getIfaceTxBytes(String iface) {
        return (iface != null) ? read(iface, "tx_bytes") : -1;
    }

    public static long getMobileRxBytes(int type) {
        return sum(MOBILE_INTERFACES, type, "rx_bytes");
    }

    public static long getMobileTxBytes(int type) {
        return sum(MOBILE_INTERFACES, type, "tx_bytes");
    }

    public static long getWifiRxBytes(int type) {
        return sum(WIFI_INTERFACES, type, "rx_bytes");
    }

    public static long getWifiTxBytes(int type) {
        return sum(WIFI_INTERFACES, type, "tx_bytes");
    }

    private static long sum(String[][] interfaces, int type, String counter) {
        if (type < 1 || type > interfaces.length) {
            return 0;
        }
        long total = 0;
        for (String iface : interfaces[type - 1]) {
            total += read(iface, counter);
        }
        return total;
    }

    private static long read(String iface, String counter) {
        String path = "/sys/class/net/" + iface + "/statistics/" + counter;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            return (line != null) ? Long.parseLong(line.trim()) : 0;
        } catch (IOException | NumberFormatException ex) {
            return 0;
        }
    }

}
