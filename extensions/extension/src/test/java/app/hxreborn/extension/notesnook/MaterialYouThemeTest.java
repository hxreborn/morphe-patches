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
package app.hxreborn.extension.notesnook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class MaterialYouThemeTest {

    private static final int ACCENT = 0x6750A4;

    private static JSONObject settings(String id) throws JSONException {
        return new JSONObject("{\"lighTheme\":{\"id\":\"" + id + "\",\"scopes\":{"
                + "\"base\":{\"primary\":{\"accent\":\"#008837\",\"accentForeground\":\"#ffffff\"},"
                + "\"selected\":{\"accent\":\"#008837\",\"border\":\"#008837\"},\"empty\":{},"
                + "\"error\":{\"accent\":\"#f54b42\"}}," + "\"sheet\":{\"selected\":{\"paragraph\":\"#008837\"}}}}}");
    }

    private static String color(JSONObject settings, String scope, String variant, String name) throws JSONException {
        return settings.getJSONObject("lighTheme")
            .getJSONObject("scopes")
            .getJSONObject(scope)
            .getJSONObject(variant)
            .getString(name);
    }

    @Test
    public void recolorsTheDefaultThemeAccent() throws JSONException {
        // given
        JSONObject settings = settings("default-light");

        // when
        boolean changed = MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertTrue(changed);
        assertEquals("#6750a4", color(settings, "base", "primary", "accent"));
    }

    @Test
    public void recolorsEveryRoleHoldingTheAccent() throws JSONException {
        // given
        JSONObject settings = settings("default-light");

        // when
        MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertEquals("#6750a4", color(settings, "base", "selected", "border"));
        assertEquals("#6750a4", color(settings, "sheet", "selected", "paragraph"));
    }

    @Test
    public void keepsOtherColours() throws JSONException {
        // given
        JSONObject settings = settings("default-light");

        // when
        MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertEquals("#f54b42", color(settings, "base", "error", "accent"));
        assertEquals("#ffffff", color(settings, "base", "primary", "accentForeground"));
    }

    @Test
    public void leavesAnInstalledThemeUntouched() throws JSONException {
        // given
        JSONObject settings = settings("community-theme");

        // when
        boolean changed = MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertFalse(changed);
        assertEquals("#008837", color(settings, "base", "primary", "accent"));
    }

    @Test
    public void reportsNoChangeWhenTheAccentAlreadyMatches() throws JSONException {
        // given
        JSONObject settings = settings("default-light");
        MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // when
        boolean changed = MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertFalse(changed);
    }

    @Test
    public void followsAWallpaperChange() throws JSONException {
        // given
        JSONObject settings = settings("default-light");
        MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // when
        boolean changed = MaterialYouTheme.recolor(settings, "lighTheme", "default-light", 0x2A5EA7);

        // then
        assertTrue(changed);
        assertEquals("#2a5ea7", color(settings, "base", "selected", "border"));
    }

    @Test
    public void recolorsStockAccentLeftBehindByAnEarlierRun() throws JSONException {
        // given
        JSONObject settings = settings("default-light");
        settings.getJSONObject("lighTheme")
            .getJSONObject("scopes")
            .getJSONObject("base")
            .getJSONObject("primary")
            .put("accent", "#6750a4");

        // when
        boolean changed = MaterialYouTheme.recolor(settings, "lighTheme", "default-light", ACCENT);

        // then
        assertTrue(changed);
        assertEquals("#6750a4", color(settings, "base", "selected", "border"));
    }

}
