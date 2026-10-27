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
public final class PlayInfoRewriterTest {

    private static final String SIGNED_MANIFEST = "https://sacdn.example.com/abc/index.mpd";

    @Test
    public void pointsSignedStreamsAtTheManifest() throws JSONException {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"format\":\"MP4\","
                + "\"signCookie\":\"" + SignedResourceTest.cloudFrontCookie("https://sacdn.example.com/abc/*")
                + "\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", 0, 0);

        // then
        JSONObject stream = new JSONObject(rewritten).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        assertEquals(SIGNED_MANIFEST, stream.getString("url"));
        assertEquals("DASH", stream.getString("format"));
    }

    @Test
    public void routesUnsignedStreamsThroughTheLoopbackAtTheirHighestResolution() throws JSONException {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"\","
                + "\"resolutions\":\"360,720, 1080\",\"size\":\"123\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", 2, 5);

        // then
        JSONObject stream = new JSONObject(rewritten).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        assertEquals(DashServer.buildUrl("77", 2, 5, 1080, "https://origin.example.com/f.mp4", 123, null),
                stream.getString("url"));
    }

    @Test
    public void leavesUnsignedStreamsWithoutAnEpisodeAlone() {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"\","
                + "\"resolutions\":\"720\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", -1, 0);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void leavesUnsignedStreamsWithoutAResolutionAlone() {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"\","
                + "\"resolutions\":\"\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", 0, 0);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void leavesPlayInfoThatIsNotJsonAlone() {
        // when
        String rewritten = PlayInfoRewriter.rewrite("not json", "77", 0, 0);

        // then
        assertEquals("not json", rewritten);
    }

}
