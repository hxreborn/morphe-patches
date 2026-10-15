/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings({ "unused", "unchecked" })
public final class UpnpActivity extends BrowserScreenActivity {

    private SsdpBrowser browser;

    @Override
    String screenTitle() {
        return "UPnP";
    }

    @Override
    boolean browsing() {
        return !hasArgument();
    }

    @Override
    void startBrowser(InetAddress address, Network network, NetworkInterface networkInterface) throws IOException {
        if (this.browser == null || !this.browser.local.equals(address) || !this.browser.network.equals(network)) {
            this.browser = new SsdpBrowser(address, network, networkInterface, this::refresh);
        }
        this.browser.start();
    }

    @Override
    void stopBrowser() {
        if (this.browser != null) {
            this.browser.stop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (browsing()) {
            addToolbarIcon(MaterialIcons.MORE, "More Options", (view) -> ExportMenu.show(this, view,
                    new UpnpExport((this.browser != null) ? this.browser.devices() : Collections.emptyList())));
        }
    }

    @Override
    void buildContent() {
        if (browsing() && showUnavailable()) {
            return;
        }
        final List<UpnpDevice> devices;
        if (browsing()) {
            devices = (this.browser != null) ? this.browser.devices() : Collections.<UpnpDevice>emptyList();
        } else {
            final Object nestedDevices = argument();
            devices = (nestedDevices != null) ? (List<UpnpDevice>) nestedDevices : Collections.<UpnpDevice>emptyList();
        }
        for (UpnpDevice device : devices) {
            if (matches(ipFilter(), device.address)) {
                addDevice(device);
            }
        }
    }

    private void addDevice(UpnpDevice device) {
        section((device.address != null) ? device.address.getHostAddress() : "");
        final LinearLayout column = infoColumn();
        nameLine(column, device.friendlyName);
        propertyLines(column, device.props);
        addRow(column);
        if (!device.services.isEmpty()) {
            clickableRow("Services", "", (view) -> startActivity(new Intent(this, UpnpServicesActivity.class)
                .putExtra(EXTRA_ARGUMENT, ScreenArguments.put(device.services))));
        }
        if (!device.devices.isEmpty()) {
            clickableRow("Devices", "", (view) -> startActivity(new Intent(this, UpnpActivity.class)
                .putExtra(EXTRA_ARGUMENT, ScreenArguments.put(device.devices))));
        }
    }

}
