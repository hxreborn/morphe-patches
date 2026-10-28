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
package hx.util;

import java.io.IOException;

import hx.testing.LocalCdn;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteFileTest {

    private LocalCdn server;

    @Before
    public void startServer() throws IOException {
        this.server = new LocalCdn(null);
        this.server.put("/file.mp4", new byte[1234]);
    }

    @After
    public void stopServer() {
        this.server.close();
    }

    @Test
    public void readsTheTotalFromTheContentRange() throws IOException {
        // when
        long length = RemoteFile.probeLength(this.server.url("/file.mp4"));

        // then
        Assertions.assertThat(length).isEqualTo(1234);
    }

    @Test
    public void readsTheContentLengthOfAnOriginThatIgnoresRanges() throws IOException {
        // given
        this.server.ignoreRanges();

        // when
        long length = RemoteFile.probeLength(this.server.url("/file.mp4"));

        // then
        Assertions.assertThat(length).isEqualTo(1234);
    }

    @Test
    public void refusesAMissingFile() {
        // when
        Throwable failure = Assertions.catchThrowable(() -> RemoteFile.probeLength(this.server.url("/missing")));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class).hasMessage("HTTP 404");
    }

}
