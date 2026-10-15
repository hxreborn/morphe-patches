/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.net.InetAddress;
import java.util.Map;

final class UpnpService {

    final String serviceType;

    final InetAddress address;

    final Map<String, String> props;

    UpnpService(String serviceType, InetAddress address, Map<String, String> props) {
        this.serviceType = serviceType;
        this.address = address;
        this.props = props;
    }

}
