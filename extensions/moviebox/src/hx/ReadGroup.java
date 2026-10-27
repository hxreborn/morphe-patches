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
