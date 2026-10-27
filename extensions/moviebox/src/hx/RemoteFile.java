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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class RemoteFile {

    private static final int PROBE_TIMEOUT_MS = 10000;

    private RemoteFile() {
    }

    static long probeLength(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(PROBE_TIMEOUT_MS);
        connection.setReadTimeout(PROBE_TIMEOUT_MS);
        connection.setRequestProperty("Range", "bytes=0-0");
        try {
            return lengthOf(connection);
        } finally {
            connection.disconnect();
        }
    }

    static long totalFromContentRange(String contentRange) {
        int slash = contentRange.indexOf('/');
        return Long.parseLong(contentRange.substring(slash + 1).trim());
    }

    private static long contentLength(HttpURLConnection connection) {
        String header = connection.getHeaderField("Content-Length");
        try {
            return (header != null) ? Long.parseLong(header.trim()) : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static long lengthOf(HttpURLConnection connection) throws IOException {
        int code = connection.getResponseCode();
        String contentRange = connection.getHeaderField("Content-Range");
        if (code == HttpURLConnection.HTTP_PARTIAL && contentRange != null && contentRange.indexOf('/') >= 0) {
            return totalFromContentRange(contentRange);
        }
        if (code == HttpURLConnection.HTTP_OK) {
            return contentLength(connection);
        }
        throw new IOException("HTTP " + code);
    }

}
