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
package app.hxreborn.extension.quranify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ApiRequest {

    private static final Set<String> FORWARDED_HEADERS = Set.of(
            "accept",
            "authorization",
            "content-type",
            "if-none-match",
            "x-billing-environment",
            "x-device-id",
            "x-session-token",
            "x-timezone");

    private static final String CONTENT_LENGTH = "content-length";

    private static final int MAX_LINE_BYTES = 8192;

    private static final int MAX_BODY_BYTES = 1024 * 1024;

    private static final int REQUEST_LINE_PARTS = 3;

    final String method;

    final String path;

    final String query;

    final Map<String, String> headers;

    final byte[] body;

    private ApiRequest(String method, String target, Map<String, String> headers, byte[] body) {
        int queryStart = target.indexOf('?');
        this.method = method;
        this.path = (queryStart < 0) ? target : target.substring(0, queryStart);
        this.query = (queryStart < 0) ? "" : target.substring(queryStart);
        this.headers = headers;
        this.body = body;
    }

    static ApiRequest read(InputStream input) throws IOException {
        String[] requestLine = readLine(input).split(" ", REQUEST_LINE_PARTS);
        if (requestLine.length != REQUEST_LINE_PARTS) {
            throw new IOException("Invalid API request line");
        }
        Map<String, String> headers = new HashMap<>();
        int contentLength = 0;
        for (String line = readLine(input); !line.isEmpty(); line = readLine(input)) {
            int separator = line.indexOf(':');
            if (separator < 1) {
                throw new IOException("Invalid API header");
            }
            String name = line.substring(0, separator).toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            if (name.equals(CONTENT_LENGTH)) {
                contentLength = Integer.parseInt(value);
            }
            if (FORWARDED_HEADERS.contains(name)) {
                headers.put(name, value);
            }
        }
        return new ApiRequest(requestLine[0], requestLine[1], headers, readBody(input, contentLength));
    }

    private static byte[] readBody(InputStream input, int length) throws IOException {
        if (length < 0 || length > MAX_BODY_BYTES) {
            throw new IOException("Invalid API request length");
        }
        byte[] body = new byte[length];
        for (int offset = 0; offset < length;) {
            int count = input.read(body, offset, length - offset);
            if (count < 0) {
                throw new IOException("Incomplete API request");
            }
            offset += count;
        }
        return body;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (line.size() < MAX_LINE_BYTES) {
            int value = input.read();
            if (value < 0) {
                throw new IOException("Incomplete API headers");
            }
            if (value == '\n') {
                String text = line.toString(StandardCharsets.US_ASCII.name());
                return text.endsWith("\r") ? text.substring(0, text.length() - 1) : text;
            }
            line.write(value);
        }
        throw new IOException("API header too large");
    }

}
