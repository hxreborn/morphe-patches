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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hx.loopback.DashServer;
import hx.okhttp.ApiClient;
import hx.source.MovieBoxSource;
import hx.testing.LocalCdn;
import hx.testing.PlayInfoFixtures;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashInterceptorTest {

    private static final String PROFILE = "/wefeed-mobile-bff/user-api/profile";

    private static final String PLAY_INFO = "/wefeed-mobile-bff/subject-api/play-info/v2";

    private static final String RESOURCE_LIST = "/wefeed-mobile-bff/subject-api/resource/v2";

    private static final String BOTTOM_TAB = "/wefeed-mobile-bff/subject-api/bottom-tab";

    private static final String MEMBER = "{\"data\":{\"daysLeft\":3,\"memberType\":\"FREE\","
            + "\"expiryDate\":\"2026-01-01\"}}";

    private static final String UNSIGNED_PLAY_INFO = "{\"data\":{\"streams\":[{\"url\":"
            + "\"https://origin.example.com/a.mp4\",\"resolutions\":\"720\",\"size\":100,\"signCookie\":\"\"}]}}";

    private final ClassLoader loader = getClass().getClassLoader();

    private LocalCdn server;

    @Before
    public void startServer() throws IOException {
        this.server = new LocalCdn(null);
    }

    @After
    public void stopServer() {
        this.server.close();
    }

    @Test
    public void extendsTheMembershipInTheProfileResponse()
            throws JSONException, ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        this.server.put(PROFILE, MEMBER.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PROFILE, true, Collections.<String>emptySet());

        // then
        Assertions.assertThat(new JSONObject(body).getJSONObject("data").getInt("daysLeft")).isEqualTo(9999);
    }

    @Test
    public void leavesOtherResponsesAlone() throws ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        this.server.put("/other", MEMBER.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch("/other", true, Collections.<String>emptySet());

        // then
        Assertions.assertThat(body).isEqualTo(MEMBER);
    }

    @Test
    public void pointsSignedStreamsAtTheManifest()
            throws JSONException, ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        String cookie = PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/abc/*");
        this.server.put(PLAY_INFO, PlayInfoFixtures.playInfo(cookie).getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PLAY_INFO + "?subjectId=77&se=0&ep=0", true, Collections.<String>emptySet());

        // then
        JSONObject stream = new JSONObject(body).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        Assertions.assertThat(stream.getString("url")).isEqualTo("https://sacdn.example.com/abc/index.mpd");
        Assertions.assertThat(stream.getString("format")).isEqualTo("DASH");
    }

    @Test
    public void routesUnsignedStreamsThroughTheLoopbackWhenDownloadsAreRouted()
            throws JSONException, ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        this.server.put(PLAY_INFO, UNSIGNED_PLAY_INFO.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PLAY_INFO + "?subjectId=77&se=1&ep=2", true, Collections.<String>emptySet());

        // then
        JSONObject stream = new JSONObject(body).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        Assertions.assertThat(stream.getString("url"))
            .isEqualTo(DashServer.buildUrl("77", 1, 2, 720, "https://origin.example.com/a.mp4", 100, null));
    }

    @Test
    public void keepsUnsignedStreamsWhenDownloadsAreNotRouted()
            throws ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        this.server.put(PLAY_INFO, UNSIGNED_PLAY_INFO.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PLAY_INFO + "?subjectId=77&se=1&ep=2", false, Collections.<String>emptySet());

        // then
        Assertions.assertThat(body).isEqualTo(UNSIGNED_PLAY_INFO);
    }

    @Test
    public void routesResourceListsThroughTheLoopbackWhenDownloadsAreRouted()
            throws JSONException, ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,"
                + "\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";
        this.server.put(RESOURCE_LIST, list.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(RESOURCE_LIST, true, Collections.<String>emptySet());

        // then
        JSONObject item = new JSONObject(body).getJSONObject("data").getJSONArray("list").getJSONObject(0);
        Assertions.assertThat(item.getString("resourceLink"))
            .isEqualTo(DashServer.buildUrl("77", 1, 2, 720, "https://origin.example.com/a.mp4", 0, null));
    }

    @Test
    public void keepsResourceListsWhenDownloadsAreNotRouted()
            throws ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,"
                + "\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";
        this.server.put(RESOURCE_LIST, list.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(RESOURCE_LIST, false, Collections.<String>emptySet());

        // then
        Assertions.assertThat(body).isEqualTo(list);
    }

    @Test
    public void removesTheHiddenHomeTabs() throws ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        String tabs = "{\"data\":{\"bottomTabs\":[{\"subTabs\":[{\"tabCode\":\"home\"},{\"tabCode\":\"live\"}]}]}}";
        this.server.put(BOTTOM_TAB, tabs.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(BOTTOM_TAB, true, new HashSet<>(Arrays.asList("live")));

        // then
        Assertions.assertThat(body.contains("home")).as(body).isTrue();
        Assertions.assertThat(!body.contains("live")).as(body).isTrue();
    }

    @Test
    public void keepsTheHomeTabsWhenNoneAreHidden()
            throws ClassNotFoundException, IOException, ReflectiveOperationException {
        // given
        String tabs = "{\"data\":{\"bottomTabs\":[{\"subTabs\":[{\"tabCode\":\"live\"}]}]}}";
        this.server.put(BOTTOM_TAB, tabs.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(BOTTOM_TAB, true, Collections.<String>emptySet());

        // then
        Assertions.assertThat(body).isEqualTo(tabs);
    }

    private String fetch(String pathAndQuery, boolean routeDownloads, Set<String> hiddenTabs) throws IOException {
        ApiClient api = new ApiClient(this.loader, new OkHttpClient(), this.server.url(""));
        DashInterceptor interceptor = new DashInterceptor(this.loader,
                new MovieBoxSource(api, (path, accepted) -> null), routeDownloads, hiddenTabs);
        Interceptor proxy = (Interceptor) Proxy.newProxyInstance(this.loader, new Class<?>[] { Interceptor.class },
                interceptor);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(proxy).build();
        Request request = new Request.Builder().url(this.server.url(pathAndQuery)).build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

}
