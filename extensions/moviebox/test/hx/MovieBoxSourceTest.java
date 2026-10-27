/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Assert;
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
        Assert.assertNull(file);
    }

    @Test
    public void refusesWithoutASignedResourceWhenTheOriginSizeDiffers() {
        // when
        IOException failure = Assert.assertThrows(IOException.class,
                () -> this.source.open("77", 1, 2, 720, this.server.url("/origin.mp4"), ORIGIN_SIZE + 1, "r1", false));

        // then
        Assert.assertTrue(failure instanceof DashServer.UnavailableException);
    }

}
