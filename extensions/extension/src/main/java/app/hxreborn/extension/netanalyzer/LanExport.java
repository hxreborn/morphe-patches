/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
