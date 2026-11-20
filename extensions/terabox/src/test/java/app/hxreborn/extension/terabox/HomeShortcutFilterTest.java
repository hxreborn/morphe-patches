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
package app.hxreborn.extension.terabox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class HomeShortcutFilterTest {

    private static final String HOME_CONFIG = "{\"more\":{\"type\":\"homecard_more\"},\"items\":["
            + "{\"type\":\"photos\"},{\"type\":\"hotlist\"},{\"type\":\"drama\"},{\"type\":\"video\"}]}";

    private static final String MORE_CONFIG = "[{\"title_key_android\":\"home_lv2_group_creation\",\"items\":["
            + "{\"type\":\"ai_video\"},{\"type\":\"tarot\"}]},"
            + "{\"title_key_android\":\"home_lv2_group_office\",\"items\":["
            + "{\"type\":\"ai_scan\"},{\"type\":\"ai_translate\"}]}]";

    private static final String SEARCH_CONFIG = "[{\"type\":\"ai_scan\"},{\"type\":\"drama\"}]";

    @Test
    public void removesHomeShortcutsOfTheGivenTypes() throws JSONException {
        String filtered = HomeShortcutFilter.removeShortcuts(HOME_CONFIG, "hotlist,drama");

        Assert.assertEquals("[\"photos\",\"video\"]", types(new JSONObject(filtered).getJSONArray("items")));
    }

    @Test
    public void keepsTheMoreShortcut() throws JSONException {
        String filtered = HomeShortcutFilter.removeShortcuts(HOME_CONFIG, "hotlist,drama");

        Assert.assertEquals("homecard_more", new JSONObject(filtered).getJSONObject("more").getString("type"));
    }

    @Test
    public void removesShortcutsInsideEveryGroup() throws JSONException {
        String filtered = HomeShortcutFilter.removeShortcuts(MORE_CONFIG, "ai_translate");

        JSONArray groups = new JSONArray(filtered);
        Assert.assertEquals("[\"ai_video\",\"tarot\"]", types(groups.getJSONObject(0).getJSONArray("items")));
        Assert.assertEquals("[\"ai_scan\"]", types(groups.getJSONObject(1).getJSONArray("items")));
    }

    @Test
    public void removesShortcutsFromAFlatList() throws JSONException {
        String filtered = HomeShortcutFilter.removeShortcuts(SEARCH_CONFIG, "drama");

        Assert.assertEquals("[\"ai_scan\"]", types(new JSONArray(filtered)));
    }

    @Test
    public void removesGroupsByTitleKey() throws JSONException {
        String filtered = HomeShortcutFilter.removeShortcutGroups(MORE_CONFIG, "home_lv2_group_creation");

        JSONArray groups = new JSONArray(filtered);
        Assert.assertEquals(1, groups.length());
        Assert.assertEquals("home_lv2_group_office", groups.getJSONObject(0).getString("title_key_android"));
    }

    @Test
    public void returnsAConfigThatIsNotJsonUnchanged() {
        String filtered = HomeShortcutFilter.removeShortcuts("not json", "drama");

        Assert.assertEquals("not json", filtered);
    }

    @Test
    public void returnsAMissingConfigUnchanged() {
        String filtered = HomeShortcutFilter.removeShortcuts(null, "drama");

        Assert.assertNull(filtered);
    }

    private static String types(JSONArray items) throws JSONException {
        JSONArray types = new JSONArray();
        for (int i = 0; i < items.length(); i++) {
            types.put(items.getJSONObject(i).getString("type"));
        }
        return types.toString();
    }

}
