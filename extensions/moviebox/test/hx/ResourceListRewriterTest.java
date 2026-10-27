/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ResourceListRewriterTest {

    @Test
    public void routesEachItemThroughTheLoopbackWithItsResourceId() throws JSONException {
        // given
        String body = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,"
                + "\"resourceId\":\"r1\",\"size\":\"500\",\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        JSONObject item = new JSONObject(rewritten).getJSONObject("data").getJSONArray("list").getJSONObject(0);
        assertEquals(DashServer.buildUrl("77", 1, 2, 720, "https://origin.example.com/a.mp4", 500, "r1"),
                item.getString("resourceLink"));
    }

    @Test
    public void omitsTheResourceIdWhenTheItemHasNone() throws JSONException {
        // given
        String body = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":0,\"ep\":0,\"resolution\":1080,"
                + "\"resourceLink\":\"https://origin.example.com/a.mp4\"}]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        JSONObject item = new JSONObject(rewritten).getJSONObject("data").getJSONArray("list").getJSONObject(0);
        assertEquals(DashServer.buildUrl("77", 0, 0, 1080, "https://origin.example.com/a.mp4", 0, null),
                item.getString("resourceLink"));
    }

    @Test
    public void keepsItemsWithoutAResourceLink() {
        // given
        String body = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":0,\"ep\":0,\"resolution\":1080}]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void keepsItemsWithAnInvalidEpisodeOrResolution() {
        // given
        String body = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"se\":-1,\"ep\":0,\"resolution\":1080,"
                + "\"resourceLink\":\"https://o.example.com/a.mp4\"},{\"se\":0,\"ep\":0,\"resolution\":0,"
                + "\"resourceLink\":\"https://o.example.com/b.mp4\"}]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void keepsItemsThatLackTheFieldsTheRouteNeeds() {
        // given
        String body = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"resourceLink\":\"https://o.example.com/a.mp4\"}]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void keepsAListWithoutASubject() {
        // given
        String body = "{\"data\":{\"list\":[]}}";

        // when
        String rewritten = ResourceListRewriter.rewrite(body);

        // then
        assertSame(body, rewritten);
    }

}
