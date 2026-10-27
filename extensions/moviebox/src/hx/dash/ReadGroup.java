/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.dash;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

final class ReadGroup {

    private final Set<HttpURLConnection> open = new HashSet<>();

    private boolean closed;

    synchronized void add(HttpURLConnection connection) throws InterruptedIOException {
        if (this.closed) {
            connection.disconnect();
            throw new InterruptedIOException("read cancelled");
        }
        this.open.add(connection);
    }

    synchronized void remove(HttpURLConnection connection) {
        this.open.remove(connection);
    }

    synchronized boolean isClosed() {
        return this.closed;
    }

    synchronized void close() {
        this.closed = true;
        for (HttpURLConnection connection : this.open) {
            connection.disconnect();
        }
        this.open.clear();
    }

}
