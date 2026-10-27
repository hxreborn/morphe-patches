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

import android.content.Context;

final class LanExport extends TableExport {

    private static final String[] HEADER = { "IP Address", "Name", "IPv6 Addresses", "MAC", "Vendor", "Properties",
            "mDNS Name", "LLMNR Name", "NetBIOS Name", "NetBIOS Domain", "SNMP Name", "DNS Name" };

    private static final String LIST_SEPARATOR = ", ";

    private final LanScan scan;

    LanExport(LanScan scan) {
        this.scan = scan;
    }

    @Override
    public String subject(Context context) {
        return "LAN Scan of '" + ((this.scan.name != null) ? this.scan.name : "") + "'";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        for (LanRecord record : this.scan.devices) {
            table.add(record.ip())
                .add(this.scan.name(record))
                .addRaw(String.join(LIST_SEPARATOR, record.addresses6))
                .add(this.scan.mac(record))
                .add(this.scan.vendor(context, record))
                .add(properties(record))
                .add(record.mdnsName)
                .add(record.llmnrName)
                .add(record.netbiosName)
                .add(record.netbiosDomain)
                .add(record.snmpName)
                .add(record.dnsName)
                .endRow();
        }
    }

    private static String properties(LanRecord record) {
        final List<String> labels = new ArrayList<>();
        addIf(labels, record.pingable, "Pingable");
        addIf(labels, record.upnp, "UPnP");
        addIf(labels, record.bonjour, "Bonjour");
        addIf(labels, record.scanning, "Scanning device");
        addIf(labels, record.gateway, "Gateway");
        addIf(labels, record.webProtocol != null, "Web interface");
        return String.join(LIST_SEPARATOR, labels);
    }

    private static void addIf(List<String> labels, boolean condition, String label) {
        if (condition) {
            labels.add(label);
        }
    }

}
