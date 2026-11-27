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
package app.hxreborn.extension.narababy;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SubscriptionCardsTest {

    @Test
    public void removesSubscriptionCards() throws Exception {
        String input = "{\"pages\":{\"activity-card/sale\":{\"card\":{\"sections\":[{\"actions\":[{\"type\":\"navigate\",\"route\":\"/account/subscription\"}]}]}},"
                + "\"activity-card/guide\":{\"card\":{\"sections\":[]}},\"guides/example\":{\"card\":{\"route\":\"/account/subscription\"}}},\"utm\":{\"variant\":\"default\"}}";
        Response response = response("https://example.com/dynamicPage?path=pages%2Factivity-card", input);
        JSONObject result = new JSONObject(SubscriptionCards.removeSubscriptionCards(response).body().string());
        JSONObject pages = result.getJSONObject("pages");
        Assert.assertFalse(pages.has("activity-card/sale"));
        Assert.assertTrue(pages.has("activity-card/guide"));
        Assert.assertTrue(pages.has("guides/example"));
        Assert.assertEquals("default", result.getJSONObject("utm").getString("variant"));
    }

    @Test
    public void preservesCardsWithoutNavigationAction() {
        String input = "{\"pages\":{\"activity-card/guide\":{\"card\":{\"route\":\"/account/subscription\"}}}}";
        Response response = response("https://example.com/dynamicPage", input);
        Assert.assertSame(response, SubscriptionCards.removeSubscriptionCards(response));
    }

    @Test
    public void preservesOtherResponsePaths() {
        Response response = response("https://example.com/other", "{\"pages\":{\"activity-card/sale\":{}}}");
        Assert.assertSame(response, SubscriptionCards.removeSubscriptionCards(response));
    }

    @Test
    public void preservesMalformedResponses() {
        Response response = response("https://example.com/dynamicPage", "activity-card/ /account/subscription");
        Assert.assertSame(response, SubscriptionCards.removeSubscriptionCards(response));
    }

    @Test
    public void preservesResponsesAboveReadLimit() {
        String body = "{\"pages\":{\"activity-card/sale\":{\"card\":{\"type\":\"navigate\",\"route\":\"/account/subscription\"}}},\"padding\":\""
                + "a".repeat(1048576) + "\"}";
        Response response = response("https://example.com/dynamicPage", body);
        Assert.assertSame(response, SubscriptionCards.removeSubscriptionCards(response));
    }

    private static Response response(String url, String body) {
        return new Response.Builder().request(new Request.Builder().url(url).build()).protocol(Protocol.HTTP_1_1)
            .code(200).message("OK").body(ResponseBody.create(null, body)).build();
    }

}
