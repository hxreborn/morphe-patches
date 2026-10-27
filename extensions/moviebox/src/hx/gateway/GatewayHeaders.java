/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.gateway;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

final class GatewayHeaders {

    private static final String WITHHELD_CLIENT_ADDRESS = "unknown";

    private final String clientInfo;

    private final BuildTag buildTag;

    GatewayHeaders(String clientInfo, BuildTag buildTag) {
        this.clientInfo = clientInfo;
        this.buildTag = buildTag;
    }

    Map<String, String> create(byte[] signingKey, String method, String pathAndQuery, String body, String token)
            throws IOException {
        long timestamp = System.currentTimeMillis();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", GatewaySignature.JSON_TYPE);
        headers.put("Content-Type", GatewaySignature.JSON_TYPE);
        headers.put("x-client-token", GatewaySignature.clientToken(timestamp));
        headers.put("x-tr-signature", GatewaySignature.create(signingKey, method, pathAndQuery, body, timestamp));
        String tag = (this.buildTag != null) ? this.buildTag.next() : null;
        headers.put("x-client-info", (tag != null) ? BuildTag.addTo(this.clientInfo, tag) : this.clientInfo);
        if (tag != null) {
            headers.put(BuildTag.HEADER, tag);
        }
        headers.put("x-forwarded-for", WITHHELD_CLIENT_ADDRESS);
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

}
