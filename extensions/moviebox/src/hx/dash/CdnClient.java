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
package hx.dash;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.util.Log;
import hx.util.Logs;
import hx.util.Streams;

final class CdnClient {

    private static final int CONNECT_TIMEOUT_MS = 15000;

    private static final int READ_TIMEOUT_MS = 30000;

    private static final int ATTEMPTS = 5;

    private static final long RETRY_DELAY_MS = 2000L;

    private CdnClient() {
    }

    static <T> T withRetries(CdnRequest<T> request) throws IOException {
        for (int attempt = 1;; attempt++) {
            try {
                return request.send();
            } catch (IOException ex) {
                if (attempt == ATTEMPTS || isCancelled(ex)) {
                    throw ex;
                }
                TransferLog.SHARED.retry();
                Log.w(Logs.TAG, "CDN read attempt " + attempt + " of " + ATTEMPTS + " failed", ex);
                pauseBeforeRetry();
            }
        }
    }

    static byte[] fetch(String url, CdnCookies cookies) throws IOException {
        try (InputStream in = connect(url, null, cookies, null).getInputStream()) {
            return Streams.readAll(in);
        }
    }

    static HttpURLConnection connect(String url, String range, CdnCookies cookies, ReadGroup reads) throws IOException {
        String cookie = cookies.current();
        HttpURLConnection connection = request(url, range, cookie, reads);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            connection.disconnect();
            if (reads != null) {
                reads.remove(connection);
            }
            connection = request(url, range, cookies.refresh(cookie), reads);
        }
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            connection.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        return connection;
    }

    private static boolean isCancelled(IOException failure) {
        return failure instanceof InterruptedIOException && !(failure instanceof SocketTimeoutException);
    }

    private static void pauseBeforeRetry() throws IOException {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
    }

    private static HttpURLConnection request(String url, String range, String cookie, ReadGroup reads)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Cookie", cookie);
        if (range != null) {
            connection.setRequestProperty("Range", range);
        }
        if (reads != null) {
            reads.add(connection);
        }
        return connection;
    }

}
