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
package hx.gateway;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.text.TextUtils;
import android.util.Base64;

final class GatewaySignature {

    static final String JSON_TYPE = "application/json";

    private static final String MAC_ALGORITHM = "HmacMD5";

    private GatewaySignature() {
    }

    static String clientToken(long timestamp) throws IOException {
        String reversed = new StringBuilder(String.valueOf(timestamp)).reverse().toString();
        try {
            return timestamp + "," + Digests.md5Hex(reversed.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    static String create(byte[] signingKey, String method, String pathAndQuery, String body, long timestamp)
            throws IOException {
        byte[] bodyBytes = (body != null) ? body.getBytes(StandardCharsets.UTF_8) : null;
        try {
            String canonical = method + "\n" + JSON_TYPE + "\n" + JSON_TYPE + "\n"
                    + ((bodyBytes != null) ? String.valueOf(bodyBytes.length) : "") + "\n" + timestamp + "\n"
                    + ((bodyBytes != null) ? Digests.md5Hex(bodyBytes) : "") + "\n" + canonicalPath(pathAndQuery);
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, MAC_ALGORITHM));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return timestamp + "|2|" + Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (GeneralSecurityException ex) {
            throw new IOException(ex);
        }
    }

    private static String canonicalPath(String pathAndQuery) throws IOException {
        int question = pathAndQuery.indexOf('?');
        if (question < 0) {
            return pathAndQuery;
        }
        List<String> pairs = new ArrayList<>();
        for (String pair : pathAndQuery.substring(question + 1).split("&")) {
            if (!pair.isEmpty()) {
                pairs.add(decode(pair));
            }
        }
        Collections.sort(pairs, (a, b) -> queryKey(a).compareTo(queryKey(b)));
        return pathAndQuery.substring(0, question + 1) + TextUtils.join("&", pairs);
    }

    private static String decode(String pair) throws IOException {
        int equals = pair.indexOf('=');
        if (equals < 0) {
            return URLDecoder.decode(pair, "UTF-8");
        }
        return URLDecoder.decode(pair.substring(0, equals), "UTF-8") + "="
                + URLDecoder.decode(pair.substring(equals + 1), "UTF-8");
    }

    private static String queryKey(String pair) {
        int equals = pair.indexOf('=');
        return (equals < 0) ? pair : pair.substring(0, equals);
    }

}
