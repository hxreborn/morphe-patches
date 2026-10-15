/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings({ "unused", "unchecked" })
public final class UpnpServicesActivity extends BrowserScreenActivity {

    @Override
    String screenTitle() {
        return "UPnP";
    }

    @Override
    void buildContent() {
        final Object argument = argument();
        final List<UpnpService> services = (argument != null) ? (List<UpnpService>) argument
                : Collections.<UpnpService>emptyList();
        String lastAddress = null;
        for (UpnpService service : services) {
            final String address = (service.address != null) ? service.address.getHostAddress() : "";
            if (!address.equals(lastAddress)) {
                section(address);
                lastAddress = address;
            }
            final LinearLayout column = infoColumn();
            nameLine(column, service.serviceType);
            propertyLines(column, service.props);
            addRow(column);
        }
    }

}
