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
package hx.okhttp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hx.testing.LocalCdn;
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
    public void joinsPathsToABaseWithoutASlash() {
        // when
        String url = new ApiClient(this.loader, new OkHttpClient(), "https://api.example.com").url("a/b?c=1");

        // then
        Assertions.assertThat(url).isEqualTo("https://api.example.com/a/b?c=1");
    }

    @Test
    public void joinsPathsToABaseWithASlash() {
        // when
        String url = new ApiClient(this.loader, new OkHttpClient(), "https://api.example.com/").url("a/b?c=1");

        // then
        Assertions.assertThat(url).isEqualTo("https://api.example.com/a/b?c=1");
    }

    @Test
    public void readsTheBodyOfASuccessfulResponse() throws IOException {
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
    public void rejectsAnUnsuccessfulResponse() throws IOException {
        try (LocalCdn server = new LocalCdn(null)) {
            // given
            ApiClient api = new ApiClient(this.loader, new OkHttpClient(), server.url(""));

            // when
            Throwable failure = Assertions.catchThrowable(() -> api.get(api.url("missing")));

            // then
            Assertions.assertThat(failure).isInstanceOf(IOException.class);
            Assertions.assertThat(failure.getMessage()).isEqualTo("HTTP 404 for " + api.url("missing"));
        }
    }

}
