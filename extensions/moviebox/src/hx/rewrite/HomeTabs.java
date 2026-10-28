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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import hx.platform.InstalledApp;
import hx.util.Logs;
import hx.util.Streams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HomeTabs {

    private static final String HIDDEN_TABS_ASSET = "hx_hidden_home_tabs";

    private HomeTabs() {
    }

    public static Set<String> readHidden() {
        Set<String> tabCodes = new HashSet<>();
        try (InputStream asset = InstalledApp.context().getAssets().open(HIDDEN_TABS_ASSET)) {
            for (String line : new String(Streams.readAll(asset), StandardCharsets.UTF_8).split("\n")) {
                if (!line.trim().isEmpty()) {
                    tabCodes.add(line.trim());
                }
            }
        } catch (FileNotFoundException ex) {
            return tabCodes;
        } catch (Exception ex) {
            Log.e(Logs.TAG, "cannot read " + HIDDEN_TABS_ASSET, ex);
        }
        Log.i(Logs.TAG, "hidden home tabs: " + tabCodes);
        return tabCodes;
    }

    static String withoutTabs(String body, Set<String> tabCodes) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            JSONArray bottomTabs = (data != null) ? data.optJSONArray("bottomTabs") : null;
            if (bottomTabs == null) {
                return body;
            }
            int removed = 0;
            for (int i = 0; i < bottomTabs.length(); i++) {
                removed += removeSubTabs(bottomTabs.optJSONObject(i), tabCodes);
            }
            return (removed > 0) ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "app tabs not rewritten: " + ex.getMessage());
            return body;
        }
    }

    private static int removeSubTabs(JSONObject bottomTab, Set<String> tabCodes) {
        JSONArray subTabs = (bottomTab != null) ? bottomTab.optJSONArray("subTabs") : null;
        if (subTabs == null) {
            return 0;
        }
        int removed = 0;
        for (int i = subTabs.length() - 1; i >= 0; i--) {
            JSONObject subTab = subTabs.optJSONObject(i);
            if (subTab != null && tabCodes.contains(subTab.optString("tabCode"))) {
                subTabs.remove(i);
                removed++;
            }
        }
        return removed;
    }

}
