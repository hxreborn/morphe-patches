/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class ReadGroupTest {

    @Test
    public void disconnectsEveryOpenConnectionWhenClosed() throws IOException {
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
    public void leavesRemovedConnectionsOpen() throws IOException {
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
    public void refusesAndDisconnectsAConnectionAddedAfterClose() throws IOException {
        // given
        ReadGroup group = new ReadGroup();
        group.close();
        CountingConnection late = new CountingConnection();

        // when
        try {
            group.add(late);
            Assertions.fail("expected the late connection to be refused");
        } catch (InterruptedIOException ex) {

            // then
            Assertions.assertThat(late.disconnected).isTrue();
        }
    }

    private static final class CountingConnection extends HttpURLConnection {

        boolean disconnected;

        CountingConnection() throws IOException {
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
