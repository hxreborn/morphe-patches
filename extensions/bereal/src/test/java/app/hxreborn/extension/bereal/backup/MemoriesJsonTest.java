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
package app.hxreborn.extension.bereal.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

public final class MemoriesJsonTest {

    private static final String PAGE = "{\"data\":[{\"momentId\":\"moment-1\"},"
            + "{\"momentId\":\"moment-2\"},{\"other\":1},{\"momentId\":null}]}";

    private static final String MOMENT = "{\"posts\":["
            + "{\"id\":\"post-1\",\"takenAt\":\"2026-10-26T20:29:23Z\","
            + "\"postedAt\":\"2026-10-26T20:31:00Z\",\"isMain\":true,\"isLate\":false,"
            + "\"primary\":{\"url\":\"https://cdn.example/back.webp\",\"width\":1500,\"height\":2000},"
            + "\"secondary\":{\"url\":\"https://cdn.example/front.webp\",\"width\":1500,\"height\":2000},"
            + "\"caption\":\"Lunch\",\"location\":{\"latitude\":48.8566,\"longitude\":2.35},"
            + "\"visibility\":[\"friends\"],"
            + "\"realmojis\":[{\"user\":{\"username\":\"friend1\"},\"emoji\":\"x\"}],"
            + "\"comments\":[{\"user\":{\"username\":\"friend2\"},\"content\":\"nice\"}]},"
            + "{\"id\":\"post-2\",\"primary\":{\"url\":\"https://cdn.example/b2.webp\"},"
            + "\"location\":{\"latitude\":null,\"longitude\":null}}]}";

    @Test
    public void readsMomentIdsInOrder() throws Exception {
        // when
        final List<String> ids = MemoriesJson.momentIds(PAGE);

        // then
        assertEquals(Arrays.asList("moment-1", "moment-2"), ids);
    }

    @Test
    public void readsAPostFromAMoment() throws Exception {
        // when
        final List<MemoryPost> posts = MemoriesJson.posts("moment-1", MOMENT);

        // then
        assertEquals(2, posts.size());
        final MemoryPost post = posts.get(0);
        assertEquals("post-1", post.id);
        assertEquals("moment-1", post.momentId);
        assertEquals(Instant.parse("2026-10-26T20:29:23Z").toEpochMilli(), post.takenAt);
        assertEquals("https://cdn.example/back.webp", post.back.url);
        assertEquals("https://cdn.example/front.webp", post.front.url);
        assertEquals("Lunch", post.caption);
        assertEquals(48.8566, post.location.latitude, 0.0);
        assertEquals(Collections.singletonList("friends"), post.visibility);
        assertEquals("friend1", post.realmojis.get(0).username);
        assertEquals("nice", post.comments.get(0).text);
    }

    @Test
    public void dropsNullCoordinates() throws Exception {
        // when
        final MemoryPost post = MemoriesJson.posts("moment-1", MOMENT).get(1);

        // then
        assertNull(post.location);
        assertNull(post.front);
        assertEquals(0L, post.takenAt);
    }

    @Test
    public void writesTheKeyFieldsToTheSidecar() throws Exception {
        // given
        final MemoryPost post = MemoriesJson.posts("moment-1", MOMENT).get(0);

        // when
        final JSONObject json = MemoriesJson.sidecar(post, Collections.singletonList("a.webp"));

        // then
        assertEquals("post-1", json.getString("id"));
        assertEquals("moment-1", json.getString("momentId"));
        assertEquals("2026-10-26T20:29:23Z", json.getString("takenAt"));
        assertEquals("Lunch", json.getString("caption"));
        assertEquals(2.35, json.getJSONObject("location").getDouble("longitude"), 0.0);
        assertEquals("a.webp", json.getJSONArray("files").getString(0));
        assertEquals("friend2", json.getJSONArray("comments").getJSONObject(0).getString("username"));
    }

}
