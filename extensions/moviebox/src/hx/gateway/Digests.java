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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class Digests {

    private Digests() {
    }

    static String md5Hex(byte[] input) throws NoSuchAlgorithmException {
        return hex(MessageDigest.getInstance("MD5").digest(input));
    }

    static String sha256Hex(byte[] input) throws NoSuchAlgorithmException {
        return hex(MessageDigest.getInstance("SHA-256").digest(input));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", value));
        }
        return out.toString();
    }

}
