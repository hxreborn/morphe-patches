/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings({ "unused", "unchecked" })
public final class BonjourServiceActivity extends BrowserScreenActivity {

    private static final String UNAVAILABLE_ADDRESS = "N/A";

    @Override
    String screenTitle() {
        return "Bonjour Service";
    }

    @Override
    void buildContent() {
        final Object argument = argument();
        final List<BonjourService> services = (argument != null) ? (List<BonjourService>) argument
                : Collections.<BonjourService>emptyList();
        for (BonjourService service : services) {
            if (!matches(ipFilter(), service.address4)) {
                continue;
            }
            section((service.server != null) ? service.server : "");
            final LinearLayout column = infoColumn();
            nameLine(column, service.name);
            labelPairLine(column, "IP Address:", hostAddress(service.address4), "Port:", String.valueOf(service.port));
            labelLine(column, "IPv6 Address:", hostAddress(service.address6));
            propertyLines(column, service.txt);
            this.list.addView(column, matchWidth());
        }
    }

    private static String hostAddress(InetAddress address) {
        return (address != null) ? address.getHostAddress() : UNAVAILABLE_ADDRESS;
    }

}
