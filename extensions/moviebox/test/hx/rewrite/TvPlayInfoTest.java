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

import java.nio.charset.StandardCharsets;

import android.net.Uri;
import hx.gateway.PlayInfoLookup;
import hx.testing.PlayInfoFixtures;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class TvPlayInfoTest {

    private static final String REQUEST = "https://tv.example.com/wefeed-tv-bff/subject/play-info/v2?subjectId=77&se=2&ep=5";

    private static final String MANIFEST = "https://cdn.example.com/movie/index.mpd";

    private static final byte[] ORIGINAL = ("{\"code\":0,\"data\":{\"title\":\"Episode\","
            + "\"resources\":[{\"resolution\":\"1080p\",\"url\":\"preview.mp4\"}]}}")
        .getBytes(StandardCharsets.UTF_8);

    @Test
    public void replacesDuplicateResourcesWithSignedStreams() throws Exception {
        String answer = PlayInfoFixtures.playInfo(PlayInfoFixtures.cloudFrontCookie("https://cdn.example.com/movie/*"));
        JSONObject source = new JSONObject(answer);
        source.getJSONObject("data").getJSONArray("streams").getJSONObject(0).put("resolutions", "480,720,1080");
        byte[] rewritten = TvPlayInfo.rewrite(REQUEST, ORIGINAL, (path, acceptance) -> source.toString());

        JSONObject data = new JSONObject(new String(rewritten, StandardCharsets.UTF_8)).getJSONObject("data");
        JSONObject stream = data.getJSONArray("streams").getJSONObject(0);
        Assertions.assertThat(stream.getString("url")).isEqualTo(MANIFEST);
        Assertions.assertThat(stream.getString("format")).isEqualTo("DASH");
        Assertions.assertThat(stream.getString("resolutions")).isEqualTo("480,720,1080");
        Assertions.assertThat(data.getJSONArray("resources").length()).isZero();
        Assertions.assertThat(data.getString("title")).isEqualTo("Episode");
    }

    @Test
    public void requestsTheSelectedEpisode() {
        PlayInfoLookup lookup = (path, acceptance) -> {
            Uri request = Uri.parse(path);
            Assertions.assertThat(request.getPath()).isEqualTo("/wefeed-mobile-bff/subject-api/play-info/v2");
            Assertions.assertThat(request.getQueryParameter("subjectId")).isEqualTo("77");
            Assertions.assertThat(request.getQueryParameter("se")).isEqualTo("2");
            Assertions.assertThat(request.getQueryParameter("ep")).isEqualTo("5");
            Assertions.assertThat(acceptance.accepts("{\"data\":{\"streams\":[]}}")).isFalse();
            return null;
        };
        Assertions.assertThat(TvPlayInfo.rewrite(REQUEST, ORIGINAL, lookup)).isSameAs(ORIGINAL);
    }

    @Test
    public void keepsTheTvResponseWhenLookupFails() {
        Assertions.assertThat(TvPlayInfo.rewrite(REQUEST, ORIGINAL, (path, acceptance) -> null)).isSameAs(ORIGINAL);
    }

    @Test
    public void keepsTheTvResponseForUnsignedPlayback() {
        Assertions
            .assertThat(TvPlayInfo.rewrite(REQUEST, ORIGINAL, (path, acceptance) -> PlayInfoFixtures.playInfo("")))
            .isSameAs(ORIGINAL);
    }

    @Test
    public void keepsTheTvResponseForMalformedPlayback() {
        Assertions.assertThat(TvPlayInfo.rewrite(REQUEST, ORIGINAL, (path, acceptance) -> "invalid"))
            .isSameAs(ORIGINAL);
    }

    @Test
    public void skipsRequestsWithoutAnEpisode() {
        byte[] rewritten = TvPlayInfo.rewrite(REQUEST.replace("&ep=5", ""), ORIGINAL, (path, acceptance) -> {
            throw new AssertionError(path);
        });
        Assertions.assertThat(rewritten).isSameAs(ORIGINAL);
    }

    @Test
    public void skipsOtherEndpoints() {
        byte[] rewritten = TvPlayInfo.rewrite(REQUEST.replace("play-info/v2", "resource"), ORIGINAL,
                (path, acceptance) -> {
                    throw new AssertionError(path);
                });
        Assertions.assertThat(rewritten).isSameAs(ORIGINAL);
    }

}
