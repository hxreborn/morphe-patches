/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ApiClientTest {

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    public void joinsPathsToTheBaseWithOneSlash() {
        // when
        String bare = new ApiClient(this.loader, new OkHttpClient(), "https://api.example.com").url("a/b?c=1");
        String slashed = new ApiClient(this.loader, new OkHttpClient(), "https://api.example.com/").url("a/b?c=1");

        // then
        Assertions.assertThat(bare).isEqualTo("https://api.example.com/a/b?c=1");
        Assertions.assertThat(slashed).isEqualTo("https://api.example.com/a/b?c=1");
    }

    @Test
    public void readsTheBodyOfASuccessfulResponse() throws Exception {
        try (LocalCdn server = new LocalCdn(null)) {
            // given
            server.put("/hello", "world".getBytes(StandardCharsets.UTF_8));
            ApiClient api = new ApiClient(this.loader, new OkHttpClient(), server.url(""));

            // when
            String body = api.get(api.url("hello"));

            // then
            Assertions.assertThat(body).isEqualTo("world");
        }
    }

    @Test
    public void rejectsAnUnsuccessfulResponse() throws Exception {
        try (LocalCdn server = new LocalCdn(null)) {
            // given
            ApiClient api = new ApiClient(this.loader, new OkHttpClient(), server.url(""));

            // when
            try {
                api.get(api.url("missing"));
                Assertions.fail("expected an HTTP failure");
            } catch (IOException ex) {

                // then
                Assertions.assertThat(ex.getMessage()).isEqualTo("HTTP 404 for " + api.url("missing"));
            }
        }
    }

}
