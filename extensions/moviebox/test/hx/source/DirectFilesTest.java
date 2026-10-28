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
package hx.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hx.okhttp.ApiClient;
import hx.testing.LocalCdn;
import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
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
public final class DirectFilesTest {

    private static final String POSITION = "/wefeed-mobile-bff/subject-api/resource-position/v2";

    private static final String LISTING = "/wefeed-mobile-bff/subject-api/resource/v2";

    private static final String ORIGIN = "https://origin.example.com/o.mp4";

    private LocalCdn server;

    private DirectFiles directFiles;

    @Before
    public void startServer() throws IOException {
        this.server = new LocalCdn(null);
        this.server.put("/f.mp4", new byte[2048]);
        ApiClient api = new ApiClient(getClass().getClassLoader(), new OkHttpClient(), this.server.url(""));
        this.directFiles = new DirectFiles(api);
    }

    @After
    public void stopServer() {
        this.server.close();
    }

    @Test
    public void resolvesTheDirectFileOfAResourceOnceItAnswersWithItsLength() {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + this.server.url("/f.mp4") + "\",\"size\":2048}]}}");

        // when
        String url = this.directFiles.resolveUrl("77", "r1", ORIGIN);

        // then
        Assertions.assertThat(url).isEqualTo(this.server.url("/f.mp4"));
        Assertions.assertThat(this.directFiles.knownUrl("r1")).isEqualTo(url);
    }

    @Test
    public void asksOnlyOnceForAResource() {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + this.server.url("/f.mp4") + "\"}]}}");
        this.directFiles.resolveUrl("77", "r1", ORIGIN);

        // when
        this.directFiles.resolveUrl("77", "r1", ORIGIN);

        // then
        Assertions.assertThat(this.server.hitsOn(POSITION)).isEqualTo(1);
    }

    @Test
    public void givesNoDirectFileWhenTheAppAlreadyHasThatLink() {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + ORIGIN + "\"}]}}");

        // when
        String url = this.directFiles.resolveUrl("77", "r1", ORIGIN);

        // then
        Assertions.assertThat(url).isNull();
        Assertions.assertThat(this.directFiles.knownUrl("r1")).isNull();
    }

    @Test
    public void givesNoDirectFileWhenThereIsNoCandidate() {
        // given
        putPosition("{\"data\":{\"list\":[]}}");

        // when
        String url = this.directFiles.resolveUrl("77", "r1", ORIGIN);

        // then
        Assertions.assertThat(url).isNull();
    }

    @Test
    public void givesNoDirectFileWithoutAnOrigin() {
        // when
        String url = this.directFiles.resolveUrl("77", "r1", null);

        // then
        Assertions.assertThat(url).isNull();
    }

    @Test
    public void givesNoDirectFileWithoutAResource() {
        // when
        String url = this.directFiles.resolveUrl("77", null, ORIGIN);

        // then
        Assertions.assertThat(url).isNull();
    }

    @Test
    public void fillsInTheSizeOfEachResourceListItem() throws JSONException {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + this.server.url("/f.mp4") + "\",\"size\":123456}]}}");
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"resourceId\":\"r1\",\"resourceLink\":\"" + ORIGIN
                + "\",\"size\":\"1\"}]}}";

        // when
        String sized = this.directFiles.withSizes(list);

        // then
        JSONObject item = new JSONObject(sized).getJSONObject("data").getJSONArray("list").getJSONObject(0);
        Assertions.assertThat(item.getString("size")).isEqualTo("123456");
        Assertions.assertThat(this.directFiles.knownUrl("r1")).isEqualTo(this.server.url("/f.mp4"));
    }

    @Test
    public void keepsTheDirectFileOfAResourceAfterManyOthersWereSized() {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + this.server.url("/f.mp4") + "\",\"size\":2048}]}}");
        for (int i = 0; i < 33; i++) {
            this.directFiles.withSizes(resourceList(i, i + 1));
        }

        // when
        this.directFiles.withSizes(resourceList(0, 1));

        // then
        Assertions.assertThat(this.directFiles.knownUrl("r0")).isEqualTo(this.server.url("/f.mp4"));
    }

    @Test
    public void keepsTheResourceListWhenNoItemHasADirectFile() {
        // given
        putPosition("{\"data\":{\"list\":[]}}");
        String list = "{\"data\":{\"subjectId\":\"77\",\"list\":[{\"resourceId\":\"r1\",\"size\":\"1\"}]}}";

        // when
        String sized = this.directFiles.withSizes(list);

        // then
        Assertions.assertThat(sized).isEqualTo(list);
    }

    @Test
    public void buildsPlayInfoStreamsFromTheDirectFiles() throws JSONException {
        // given
        putPosition("{\"data\":{\"list\":[{\"resourceLink\":\"" + this.server.url("/f.mp4") + "\",\"size\":2048}]}}");
        this.server.put(LISTING, ("{\"data\":{\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,\"resourceId\":\"r1\","
                + "\"resourceLink\":\"" + ORIGIN + "\",\"size\":\"2048\",\"duration\":60,\"codecName\":\"h264\"},"
                + "{\"se\":9,\"ep\":9,\"resolution\":1080,\"resourceId\":\"other\"}]}}")
            .getBytes(StandardCharsets.UTF_8));

        // when
        String playInfo = this.directFiles.playInfoFor("{\"data\":{\"streams\":[]}}", "77", 1, 2);

        // then
        JSONObject data = new JSONObject(playInfo).getJSONObject("data");
        JSONArray streams = data.getJSONArray("streams");
        Assertions.assertThat(streams.length()).isEqualTo(1);
        Assertions.assertThat(streams.getJSONObject(0).getString("url")).isEqualTo(this.server.url("/f.mp4"));
        Assertions.assertThat(streams.getJSONObject(0).getString("resolutions")).isEqualTo("720");
        Assertions.assertThat(streams.getJSONObject(0).getString("signCookie")).isEqualTo("");
        Assertions.assertThat(data.getString("displayResolutions")).isEqualTo("720");
    }

    @Test
    public void givesNoPlayInfoWhenThereIsNoDirectFile() {
        // given
        putPosition("{\"data\":{\"list\":[]}}");
        this.server.put(LISTING, ("{\"data\":{\"list\":[{\"se\":1,\"ep\":2,\"resolution\":720,\"resourceId\":\"r1\","
                + "\"resourceLink\":\"" + ORIGIN + "\"}]}}")
            .getBytes(StandardCharsets.UTF_8));

        // when
        String playInfo = this.directFiles.playInfoFor("{\"data\":{\"streams\":[]}}", "77", 1, 2);

        // then
        Assertions.assertThat(playInfo).isNull();
    }

    @Test
    public void givesNoPlayInfoWithoutASubject() {
        // when
        String playInfo = this.directFiles.playInfoFor("{}", null, 1, 2);

        // then
        Assertions.assertThat(playInfo).isNull();
    }

    @Test
    public void givesNoPlayInfoWithoutASeason() {
        // when
        String playInfo = this.directFiles.playInfoFor("{}", "77", -1, 2);

        // then
        Assertions.assertThat(playInfo).isNull();
    }

    @Test
    public void givesNoPlayInfoWithoutAnEpisode() {
        // when
        String playInfo = this.directFiles.playInfoFor("{}", "77", 1, -1);

        // then
        Assertions.assertThat(playInfo).isNull();
    }

    private static String resourceList(int from, int to) {
        StringBuilder items = new StringBuilder();
        for (int i = from; i < to; i++) {
            items.append((i > from) ? "," : "")
                .append("{\"resourceId\":\"r")
                .append(i)
                .append("\",\"resourceLink\":\"")
                .append(ORIGIN)
                .append("\",\"size\":\"1\"}");
        }
        return "{\"data\":{\"subjectId\":\"77\",\"list\":[" + items + "]}}";
    }

    private void putPosition(String json) {
        this.server.put(POSITION, json.getBytes(StandardCharsets.UTF_8));
    }

}
