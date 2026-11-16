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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HttpExchange {

    private static final List<String> RETURNED_HEADERS = List.of(
            "etag",
            "cache-control",
            "retry-after",
            "location",
            "set-cookie",
            "www-authenticate",
            "x-billing-environment");

    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    private static final int BUFFER_BYTES = 8192;

    private HttpExchange() {
    }

    static ApiResponse send(String url, String method, Map<String, String> headers, byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(LocalApiServer.TIMEOUT_MS);
            connection.setReadTimeout(LocalApiServer.TIMEOUT_MS);
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept-Encoding", "identity");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (body.length > 0) {
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
            }
            int status = connection.getResponseCode();
            InputStream content = (status >= HttpURLConnection.HTTP_BAD_REQUEST) ? connection.getErrorStream()
                    : connection.getInputStream();
            Map<String, String> returnedHeaders = new HashMap<>();
            for (String name : RETURNED_HEADERS) {
                String value = connection.getHeaderField(name);
                if (value != null) {
                    returnedHeaders.put(name, value);
                }
            }
            String contentType = connection.getContentType();
            return new ApiResponse(status, (contentType != null) ? contentType : DEFAULT_CONTENT_TYPE,
                    (content != null) ? readAll(content) : new byte[0], returnedHeaders);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_BYTES];
            for (int count = stream.read(buffer); count != -1; count = stream.read(buffer)) {
                if (output.size() + count > MAX_RESPONSE_BYTES) {
                    throw new IOException("API response too large");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

}
