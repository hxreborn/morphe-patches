/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

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
