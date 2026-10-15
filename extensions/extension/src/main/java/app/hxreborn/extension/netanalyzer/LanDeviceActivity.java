/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class LanDeviceActivity extends HostScreenActivity {

    private static final String NOT_AVAILABLE = "N/A";

    private static final String YES = "Yes";

    private static final String NO = "No";

    private static final String LIST_SEPARATOR = "\n";

    private static final String LINK_LOCAL_PREFIX = "fe80:";

    private static final String MDNS_SUFFIX = ".local.";

    private static LanRecord record;

    private static LanScan scan;

    private static boolean online;

    private static Runnable queryWithTools;

    static void show(LanRecord selectedRecord, LanScan selectedScan, boolean isOnline, Runnable query) {
        record = selectedRecord;
        scan = selectedScan;
        online = isOnline;
        queryWithTools = query;
        HostActivity.open(LanDeviceActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return "Details";
    }

    @Override
    void buildContent() {
        final LanRecord device = record;
        final LanScan lanScan = scan;
        if (device == null || lanScan == null) {
            finish();
            return;
        }
        section("Actions");
        if (queryWithTools != null) {
            clickableRow("Query IP with Tools", "", (view) -> {
                queryWithTools.run();
                finish();
            });
        }
        if (device.addresses6.isEmpty()) {
            onOffRow("Query IPv6 with Tools", null);
        } else {
            clickableRow("Query IPv6 with Tools", "", (view) -> {
                if (ToolsHistory.query(device.addresses6.get(0))) {
                    finish();
                }
            });
        }
        clickableRow("Wake on LAN", "",
                (view) -> startActivity(new Intent(this, WakeOnLanActivity.class)
                    .putExtra(WakeOnLanActivity.EXTRA_MAC, nullToEmpty(lanScan.mac(device)))
                    .putExtra(WakeOnLanActivity.EXTRA_NAME, lanScan.name(device))));
        serviceRow("Bonjour Services", device.bonjour, BonjourActivity.class, device);
        serviceRow("UPnP Services", device.upnp, UpnpActivity.class, device);
        final String url = device.webUrl();
        if (online && url != null) {
            onOffClickableRow("Web Interface", YES,
                    (view) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        } else {
            onOffRow("Web Interface", null);
        }

        section("Device");
        onOffRow("IP Address", present(device.ip()));
        addressesRow(device.addresses6);
        onOffRow("MAC", present(lanScan.mac(device)));
        onOffRow("Vendor", present(lanScan.vendor(this, device)));
        onOffRow("Pingable", device.pingable ? YES : NO, Status.of(device.pingable, true));

        section("Device Names");
        final String[] assigned = LanNames.get(lanScan.networkKey(), device.ip());
        final String assignedName = (assigned != null) ? assigned[LanNames.NAME] : null;
        clickableRow("Assign Name to IP",
                (assignedName != null && !assignedName.isEmpty()) ? assignedName : NOT_AVAILABLE,
                (view) -> LanCustomNameActivity.show(lanScan.networkKey(), device.ip()));
        onOffRow("mDNS Name", present(stripMdnsSuffix(device.mdnsName)));
        onOffRow("LLMNR Name", present(device.llmnrName));
        onOffRow("NetBIOS Name", present(device.netbiosName));
        onOffRow("NetBIOS Domain", present(device.netbiosDomain));
        onOffRow("SNMP Name", present(device.snmpName));
        onOffRow("DNS Name", present(device.dnsName));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.list.removeAllViews();
        buildContent();
    }

    private void serviceRow(String title, boolean advertised, Class<?> screen, LanRecord device) {
        if (advertised && online) {
            onOffClickableRow(title, YES, (view) -> startActivity(
                    new Intent(this, screen).putExtra(BrowserScreenActivity.EXTRA_IP, device.ip())));
        } else {
            onOffRow(title, null);
        }
    }

    private void addressesRow(List<String> addresses) {
        if (addresses.isEmpty()) {
            onOffRow("IPv6 Addresses", null);
            return;
        }
        final List<String> sorted = new ArrayList<>(addresses);
        sorted.sort((first, second) -> {
            final boolean firstLinkLocal = first.startsWith(LINK_LOCAL_PREFIX);
            if (firstLinkLocal != second.startsWith(LINK_LOCAL_PREFIX)) {
                return firstLinkLocal ? -1 : 1;
            }
            return first.compareToIgnoreCase(second);
        });
        onOffRow("IPv6 Addresses", String.join(LIST_SEPARATOR, sorted));
    }

    private static String present(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    private static String stripMdnsSuffix(String name) {
        return (name != null && name.endsWith(MDNS_SUFFIX)) ? name.substring(0, name.length() - MDNS_SUFFIX.length())
                : name;
    }

    private static String nullToEmpty(String value) {
        return (value != null) ? value : "";
    }

}
