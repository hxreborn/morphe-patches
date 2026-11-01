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

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class HomeSection {

    private static final String[] AI_VIDEO_TOOLS = { "videogenerator", "imagetovideo", "veo3", "seedance" };

    private static final String[] AI_VIDEO_DEEPLINKS = { "videogen", "video-gen", "veo3", "seedance" };

    private HomeSection() {
    }

    public static void disable(JSONObject section, String name) {
        if (section == null || !name.equals(section.optString("name"))) {
            return;
        }
        section.remove("enabled");
        section.remove("content");
        section.remove("autoscroll");
    }

    public static void removeAiVideo(JSONObject section) {
        if (section == null) {
            return;
        }
        final JSONArray content = section.optJSONArray("content");
        if (content == null) {
            return;
        }
        for (int i = content.length() - 1; i >= 0; i--) {
            if (isAiVideo(content.opt(i))) {
                content.remove(i);
            }
        }
    }

    private static boolean isAiVideo(Object item) {
        if (item instanceof String) {
            return containsAny((String) item, AI_VIDEO_TOOLS);
        }
        if (item instanceof JSONObject) {
            return containsAny(((JSONObject) item).optString("deeplink"), AI_VIDEO_DEEPLINKS);
        }
        return false;
    }

    private static boolean containsAny(String value, String[] needles) {
        final String lowered = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (lowered.contains(needle)) {
                return true;
            }
        }
        return false;
    }

}
