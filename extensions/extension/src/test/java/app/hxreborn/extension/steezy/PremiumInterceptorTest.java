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
package app.hxreborn.extension.steezy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class PremiumInterceptorTest {

    private static final String API = "https://v2.api.steezy.co/graphql";

    private static final String FREE_ACCOUNT = "{\"data\":{\"me\":{\"id\":\"u1\",\"isActive\":false,"
            + "\"primarySubscription\":null,\"__typename\":\"User\"}}}";

    @Test
    public void opensEveryLockedClass() throws IOException {
        // given
        String locked = "{\"data\":{\"classById\":{\"id\":\"7\",\"canUserTakeClass\":false}}}";

        // when
        String body = fetch(API, locked);

        // then
        assertEquals("{\"data\":{\"classById\":{\"id\":\"7\",\"canUserTakeClass\":true}}}", body);
    }

    @Test
    public void activatesTheSubscriptionOfAFreeAccount() throws Exception {
        // when
        JSONObject me = new JSONObject(fetch(API, FREE_ACCOUNT)).getJSONObject("data").getJSONObject("me");

        // then
        assertTrue(me.getBoolean("isActive"));
        assertEquals("premium", me.getJSONObject("primarySubscription").getJSONObject("plan").getString("accessType"));
    }

    @Test
    public void keepsAnExistingSubscription() throws Exception {
        // given
        String subscribed = "{\"data\":{\"me\":{\"isActive\":true,\"primarySubscription\":{\"id\":\"real\"}}}}";

        // when
        JSONObject me = new JSONObject(fetch(API, subscribed)).getJSONObject("data").getJSONObject("me");

        // then
        assertEquals("real", me.getJSONObject("primarySubscription").getString("id"));
    }

    @Test
    public void leavesOtherHostsUntouched() throws IOException {
        // when
        String body = fetch("https://example.com/graphql", FREE_ACCOUNT);

        // then
        assertEquals(FREE_ACCOUNT, body);
    }

    @Test
    public void leavesNonJsonResponsesUntouched() throws IOException {
        // when
        String body = fetch(API, FREE_ACCOUNT, "text/plain");

        // then
        assertEquals(FREE_ACCOUNT, body);
    }

    private static String fetch(String url, String reply) throws IOException {
        return fetch(url, reply, "application/json");
    }

    private static String fetch(String url, String reply, String contentType) throws IOException {
        OkHttpClient.Builder builder = PremiumInterceptor.install(new OkHttpClient.Builder());
        OkHttpClient client = builder
            .addInterceptor((chain) -> new Response.Builder().request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(reply, MediaType.get(contentType)))
                .build())
            .build();
        try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            return response.body().string();
        }
    }

}
