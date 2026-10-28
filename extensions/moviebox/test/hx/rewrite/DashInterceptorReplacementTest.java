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
package hx.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hx.gateway.PlayInfoLookup;
import hx.okhttp.ApiClient;
import hx.source.MovieBoxSource;
import hx.testing.PlayInfoFixtures;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashInterceptorReplacementTest {

    private static final String PLAY_INFO_URL = "https://api6.example.com/wefeed-mobile-bff/subject-api/play-info/v2"
            + "?subjectId=77&se=1&ep=2&isVip=true";

    private static final String PLAY_INFO_PATH_AND_QUERY = "/wefeed-mobile-bff/subject-api/play-info/v2"
            + "?subjectId=77&se=1&ep=2&isVip=true";

    private static final String EDGE_MANIFEST = "https://edge.example.com/video/index.mpd";

    private static final String UNPACED_MANIFEST = "https://sacdn.hakunaymatata.com/abc/index.mpd";

    private static final String EDGE_PLAY_INFO = PlayInfoFixtures
        .playInfo(PlayInfoFixtures.edgeCacheCookie("https://edge.example.com/video/"));

    private static final String UNPACED_PLAY_INFO = PlayInfoFixtures
        .playInfo(PlayInfoFixtures.cloudFrontCookie("https://sacdn.hakunaymatata.com/abc/*"));

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<String> lookedUp = new ArrayList<>();

    @Test
    public void servesTheUnpacedPlayInfoInsteadOfTheEdgeCacheOne()
            throws IOException, ReflectiveOperationException, JSONException {
        // given
        DashInterceptor interceptor = givenInterceptorFinding(UNPACED_PLAY_INFO);

        // when
        String body = intercept(interceptor, true);

        // then
        Assertions.assertThat(firstStreamUrl(body)).isEqualTo(UNPACED_MANIFEST);
    }

    @Test
    public void asksForTheSamePlayInfoPathAndQuery() throws ReflectiveOperationException {
        // given
        DashInterceptor interceptor = givenInterceptorFinding(UNPACED_PLAY_INFO);

        // when
        intercept(interceptor, true);

        // then
        Assertions.assertThat(this.lookedUp).isEqualTo(Collections.singletonList(PLAY_INFO_PATH_AND_QUERY));
    }

    @Test
    public void keepsTheEdgeCachePlayInfoWhenNoUnpacedOneIsFound()
            throws IOException, ReflectiveOperationException, JSONException {
        // given
        DashInterceptor interceptor = givenInterceptorFinding(null);

        // when
        String body = intercept(interceptor, true);

        // then
        Assertions.assertThat(firstStreamUrl(body)).isEqualTo(EDGE_MANIFEST);
    }

    @Test
    public void keepsTheEdgeCachePlayInfoWhenTheAlternativeIsOnAnotherHost()
            throws IOException, ReflectiveOperationException, JSONException {
        // given
        DashInterceptor interceptor = givenInterceptorFinding(
                PlayInfoFixtures.playInfo(PlayInfoFixtures.cloudFrontCookie("https://other.example.com/abc/*")));

        // when
        String body = intercept(interceptor, true);

        // then
        Assertions.assertThat(firstStreamUrl(body)).isEqualTo(EDGE_MANIFEST);
    }

    @Test
    public void returnsTheStockBodyWhenTheRewriteFailsAfterAReplacement() throws ReflectiveOperationException {
        // given
        DashInterceptor interceptor = givenInterceptorFinding(UNPACED_PLAY_INFO);

        // when
        String body = intercept(interceptor, false);

        // then
        Assertions.assertThat(body).isEqualTo(EDGE_PLAY_INFO);
    }

    private DashInterceptor givenInterceptorFinding(String replacement) {
        PlayInfoLookup lookup = (pathAndQuery, accepted) -> {
            this.lookedUp.add(pathAndQuery);
            return (replacement != null && accepted.accepts(replacement)) ? replacement : null;
        };
        ApiClient api = new ApiClient(this.loader, new OkHttpClient(), "https://api6.example.com/");
        return new DashInterceptor(this.loader, new MovieBoxSource(api, lookup), true, Collections.<String>emptySet());
    }

    private String intercept(DashInterceptor interceptor, boolean responseKnowsItsRequest)
            throws ReflectiveOperationException {
        FakeBuilder builder = new FakeBuilder();
        FakeResponse response = new FakeResponse(builder, responseKnowsItsRequest);
        interceptor.intercept(new FakeChain(response));
        return new String(builder.body, StandardCharsets.UTF_8);
    }

    private static String firstStreamUrl(String playInfo) throws IOException, JSONException, IOException, IOException,
            IOException, IOException, IOException, IOException, IOException {
        return new JSONObject(playInfo).getJSONObject("data").getJSONArray("streams").getJSONObject(0).getString("url");
    }

    public static final class FakeChain {

        private final FakeRequest request = new FakeRequest();

        private final FakeResponse response;

        FakeChain(FakeResponse response) {
            this.response = response;
        }

        public FakeRequest request() {
            return this.request;
        }

        public FakeResponse proceed(FakeRequest sent) {
            return this.response;
        }

    }

    public static final class FakeRequest {

        public String url() {
            return PLAY_INFO_URL;
        }

    }

    public static final class FakeResponse {

        private final FakeBuilder builder;

        private final boolean knowsItsRequest;

        FakeResponse(FakeBuilder builder, boolean knowsItsRequest) {
            this.builder = builder;
            this.knowsItsRequest = knowsItsRequest;
        }

        public FakeBody body() {
            return new FakeBody();
        }

        public FakeBuilder newBuilder() {
            return this.builder;
        }

        public FakeRequest request() {
            if (!this.knowsItsRequest) {
                throw new IllegalStateException("response without its request");
            }
            return new FakeRequest();
        }

    }

    public static final class FakeBody {

        public byte[] bytes() {
            return EDGE_PLAY_INFO.getBytes(StandardCharsets.UTF_8);
        }

        public Object contentType() {
            return null;
        }

    }

    public static final class FakeBuilder {

        private byte[] body;

        public FakeBuilder body(ResponseBody replacement) throws IOException {
            this.body = replacement.bytes();
            return this;
        }

        public FakeBuilder removeHeader(String name) {
            return this;
        }

        public FakeResponse build() {
            return new FakeResponse(this, true);
        }

    }

}
