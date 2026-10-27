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
package hx.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hx.dash.DashFile;
import hx.loopback.DashServer;
import hx.okhttp.ApiClient;
import hx.testing.LocalCdn;
import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class MovieBoxSourceTest {

    private static final String PLAY_INFO = "/" + Endpoints.PLAY_INFO;

    private static final String POSITION = "/" + Endpoints.RESOURCE_POSITION;

    private static final int ORIGIN_SIZE = 2048;

    private LocalCdn server;

    private MovieBoxSource source;

    @Before
    public void startServerWithoutSignedStreams() throws IOException {
        this.server = new LocalCdn(null);
        this.server.put(PLAY_INFO, "{\"data\":{\"streams\":[]}}".getBytes(StandardCharsets.UTF_8));
        this.server.put(POSITION, "{\"data\":{\"list\":[]}}".getBytes(StandardCharsets.UTF_8));
        this.server.put("/origin.mp4", new byte[ORIGIN_SIZE]);
        ApiClient api = new ApiClient(getClass().getClassLoader(), new OkHttpClient(), this.server.url("/"));
        this.source = new MovieBoxSource(api, (path, accepted) -> null);
    }

    @After
    public void stopServer() {
        this.server.close();
    }

    @Test
    public void redirectsToTheOriginWithoutASignedResourceWhenItsSizeMatches() throws IOException {
        // when
        DashFile file = this.source.open("77", 1, 2, 720, this.server.url("/origin.mp4"), ORIGIN_SIZE, "r1", false);

        // then
        Assertions.assertThat(file).isNull();
    }

    @Test
    public void refusesWithoutASignedResourceWhenTheOriginSizeDiffers() {
        // when
        Throwable failure = Assertions.catchThrowable(
                () -> this.source.open("77", 1, 2, 720, this.server.url("/origin.mp4"), ORIGIN_SIZE + 1, "r1", false));

        // then
        Assertions.assertThat(failure).isInstanceOf(DashServer.UnavailableException.class);
    }

}
