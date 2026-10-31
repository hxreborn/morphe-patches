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
package app.hxreborn.extension.pixlr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class HomeSectionTest {

    private static JSONObject section(String name) throws JSONException {
        return new JSONObject().put("name", name)
            .put("enabled", true)
            .put("content", new JSONArray().put("nanoBanana"))
            .put("autoscroll", 6)
            .put("height", 120);
    }

    @Test
    public void ignoresNullSection() {
        HomeSection.disable(null, "features");
    }

    @Test
    public void disablesTheNamedSection() throws JSONException {
        // given
        final JSONObject section = section("features");

        // when
        HomeSection.disable(section, "features");

        // then
        assertFalse(section.optBoolean("enabled"));
    }

    @Test
    public void removesTheVisibilityKeysOfTheNamedSection() throws JSONException {
        // given
        final JSONObject section = section("ai_create");

        // when
        HomeSection.disable(section, "ai_create");

        // then
        assertEquals("{\"name\":\"ai_create\",\"height\":120}", section.toString());
    }

    @Test
    public void leavesOtherSectionsUntouched() throws JSONException {
        // given
        final JSONObject section = section("photo_tools");
        final String original = section.toString();

        // when
        HomeSection.disable(section, "ai_tools");

        // then
        assertEquals(original, section.toString());
    }

    @Test
    public void removesAiVideoToolsFromTheContent() throws JSONException {
        // given
        final JSONObject section = new JSONObject().put("name", "ai_create")
            .put("content", new JSONArray().put("nanoBanana").put("videoGenerator").put("veo3").put("imageGenerator"));

        // when
        HomeSection.removeAiVideo(section);

        // then
        assertEquals("[\"nanoBanana\",\"imageGenerator\"]", section.getJSONArray("content").toString());
    }

    @Test
    public void removesAiVideoBannersFromTheContent() throws JSONException {
        // given
        final JSONObject section = new JSONObject().put("name", "features")
            .put("content", new JSONArray()
                .put(new JSONObject().put("title", "Nano Banana").put("deeplink", "pixlr://aitool/nanobanana"))
                .put(new JSONObject().put("title", "AI Video").put("deeplink", "pixlr://aitool/videogen"))
                .put(new JSONObject().put("title", "Text to Speech").put("deeplink", "pixlr://app/video-editor")));

        // when
        HomeSection.removeAiVideo(section);

        // then
        final JSONArray content = section.getJSONArray("content");
        assertEquals(2, content.length());
        assertEquals("Nano Banana", content.getJSONObject(0).getString("title"));
        assertEquals("Text to Speech", content.getJSONObject(1).getString("title"));
    }

    @Test
    public void keepsSectionsWithoutContent() throws JSONException {
        // given
        final JSONObject section = new JSONObject().put("name", "projects").put("enabled", true);
        final String original = section.toString();

        // when
        HomeSection.removeAiVideo(section);

        // then
        assertEquals(original, section.toString());
    }

    @Test
    public void ignoresNullSectionWhenRemovingAiVideo() {
        HomeSection.removeAiVideo(null);
    }

}
