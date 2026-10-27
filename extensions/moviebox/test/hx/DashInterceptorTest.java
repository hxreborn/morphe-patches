/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

    private static final String MEMBER = "{\"data\":{\"daysLeft\":3,\"memberType\":\"FREE\",\"expiryDate\":\"2026-01-01\"}}";

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
    public void extendsTheMembershipInTheProfileResponse() throws Exception {
        // given
        this.server.put(PROFILE, MEMBER.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PROFILE, true, Collections.<String>emptySet());

        // then
        assertEquals(9999, new JSONObject(body).getJSONObject("data").getInt("daysLeft"));
    }

    @Test
    public void leavesOtherResponsesAlone() throws Exception {
        // given
        this.server.put("/other", MEMBER.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch("/other", true, Collections.<String>emptySet());

        // then
        assertEquals(MEMBER, body);
    }

    @Test
    public void pointsSignedStreamsAtTheManifest() throws Exception {
        // given
        String cookie = SignedResourceTest.cloudFrontCookie("https://sacdn.example.com/abc/*");
        this.server.put(PLAY_INFO, SignedResourceTest.playInfo(cookie).getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(PLAY_INFO + "?subjectId=77&se=0&ep=0", true, Collections.<String>emptySet());

        // then
        JSONObject stream = new JSONObject(body).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        assertEquals("https://sacdn.example.com/abc/index.mpd", stream.getString("url"));
        assertEquals("DASH", stream.getString("format"));
    }

    @Test
    public void routesResourceListsThroughTheLoopbackWhenDownloadsAreRouted() throws Exception {
        // given
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,"
                + "\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";
        this.server.put(RESOURCE_LIST, list.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(RESOURCE_LIST, true, Collections.<String>emptySet());

        // then
        JSONObject item = new JSONObject(body).getJSONObject("data").getJSONArray("list").getJSONObject(0);
        assertEquals(DashServer.buildUrl("77", 1, 2, 720, "https://origin.example.com/a.mp4", 0, null),
                item.getString("resourceLink"));
    }

    @Test
    public void keepsResourceListsWhenDownloadsAreNotRouted() throws Exception {
        // given
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,"
                + "\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";
        this.server.put(RESOURCE_LIST, list.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(RESOURCE_LIST, false, Collections.<String>emptySet());

        // then
        assertEquals(list, body);
    }

    @Test
    public void removesTheHiddenHomeTabs() throws Exception {
        // given
        String tabs = "{\"data\":{\"bottomTabs\":[{\"subTabs\":[{\"tabCode\":\"home\"},{\"tabCode\":\"live\"}]}]}}";
        this.server.put(BOTTOM_TAB, tabs.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(BOTTOM_TAB, true, new HashSet<>(Arrays.asList("live")));

        // then
        assertTrue(body, body.contains("home"));
        assertTrue(body, !body.contains("live"));
    }

    @Test
    public void keepsTheHomeTabsWhenNoneAreHidden() throws Exception {
        // given
        String tabs = "{\"data\":{\"bottomTabs\":[{\"subTabs\":[{\"tabCode\":\"live\"}]}]}}";
        this.server.put(BOTTOM_TAB, tabs.getBytes(StandardCharsets.UTF_8));

        // when
        String body = fetch(BOTTOM_TAB, true, Collections.<String>emptySet());

        // then
        assertEquals(tabs, body);
    }

    private String fetch(String pathAndQuery, boolean routeDownloads, Set<String> hiddenTabs) throws Exception {
        ApiClient api = new ApiClient(this.loader, new OkHttpClient(), this.server.url(""));
        DashInterceptor interceptor = new DashInterceptor(this.loader,
                new MovieBoxSource(api, (path, accepted) -> null), routeDownloads, hiddenTabs);
        Object proxy = ClientInterceptors.proxy(this.loader, interceptor);
        OkHttpClient client = (OkHttpClient) ClientInterceptors.addFirst(new OkHttpClient(), proxy);
        Request request = new Request.Builder().url(this.server.url(pathAndQuery)).build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

}
