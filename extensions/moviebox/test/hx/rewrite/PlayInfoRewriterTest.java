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

import hx.loopback.DashServer;
import hx.testing.PlayInfoFixtures;
import org.assertj.core.api.Assertions;
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
                + "\"signCookie\":\"" + PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/abc/*") + "\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", 0, 0);

        // then
        JSONObject stream = new JSONObject(rewritten).getJSONObject("data").getJSONArray("streams").getJSONObject(0);
        Assertions.assertThat(stream.getString("url")).isEqualTo(SIGNED_MANIFEST);
        Assertions.assertThat(stream.getString("format")).isEqualTo("DASH");
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
        Assertions.assertThat(stream.getString("url"))
            .isEqualTo(DashServer.buildUrl("77", 2, 5, 1080, "https://origin.example.com/f.mp4", 123, null));
    }

    @Test
    public void leavesUnsignedStreamsWithoutAnEpisodeAlone() {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"\","
                + "\"resolutions\":\"720\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", -1, 0);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesUnsignedStreamsWithoutAResolutionAlone() {
        // given
        String body = "{\"data\":{\"streams\":[{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"\","
                + "\"resolutions\":\"\"}]}}";

        // when
        String rewritten = PlayInfoRewriter.rewrite(body, "77", 0, 0);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesPlayInfoThatIsNotJsonAlone() {
        // when
        String rewritten = PlayInfoRewriter.rewrite("not json", "77", 0, 0);

        // then
        Assertions.assertThat(rewritten).isEqualTo("not json");
    }

}
