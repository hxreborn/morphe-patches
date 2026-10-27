/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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
