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

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class ReadGroupTest {

    @Test
    public void disconnectsEveryOpenConnectionWhenClosed() throws InterruptedIOException, MalformedURLException {
        // given
        ReadGroup group = new ReadGroup();
        CountingConnection first = new CountingConnection();
        CountingConnection second = new CountingConnection();
        group.add(first);
        group.add(second);

        // when
        group.close();

        // then
        Assertions.assertThat(first.disconnected).isTrue();
        Assertions.assertThat(second.disconnected).isTrue();
        Assertions.assertThat(group.isClosed()).isTrue();
    }

    @Test
    public void leavesRemovedConnectionsOpen() throws InterruptedIOException, MalformedURLException {
        // given
        ReadGroup group = new ReadGroup();
        CountingConnection finished = new CountingConnection();
        group.add(finished);
        group.remove(finished);

        // when
        group.close();

        // then
        Assertions.assertThat(finished.disconnected).isFalse();
    }

    @Test
    public void refusesAndDisconnectsAConnectionAddedAfterClose() throws MalformedURLException {
        // given
        ReadGroup group = new ReadGroup();
        group.close();
        CountingConnection late = new CountingConnection();

        // when
        Throwable failure = Assertions.catchThrowable(() -> group.add(late));

        // then
        Assertions.assertThat(failure).isInstanceOf(InterruptedIOException.class);
        Assertions.assertThat(late.disconnected).isTrue();
    }

    private static final class CountingConnection extends HttpURLConnection {

        boolean disconnected;

        CountingConnection() throws MalformedURLException {
            super(new URL("https://example.com"));
        }

        @Override
        public void disconnect() {
            this.disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

    }

}
