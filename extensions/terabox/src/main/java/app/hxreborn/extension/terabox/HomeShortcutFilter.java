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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@SuppressWarnings("unused")
public final class HomeShortcutFilter {

    private static final String TAG = "HomeShortcutFilter";

    private static final String INVALID_CONFIG_MESSAGE = "Shortcut config is not valid JSON, left unchanged: ";

    private static final String ITEMS = "items";

    private static final String TYPE = "type";

    private static final String GROUP_TITLE_KEY = "title_key_android";

    private HomeShortcutFilter() {
    }

    public static String removeShortcuts(String config, String types) {
        Set<String> removedTypes = split(types);
        try {
            Object root = parse(config);
            if (root instanceof JSONObject) {
                removeFromItems((JSONObject) root, removedTypes);
                return root.toString();
            }
            if (root instanceof JSONArray) {
                JSONArray entries = (JSONArray) root;
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject group = entries.optJSONObject(i);
                    if (group != null) {
                        removeFromItems(group, removedTypes);
                    }
                }
                return removeEntries(entries, TYPE, removedTypes).toString();
            }
        } catch (JSONException ex) {
            Log.w(TAG, INVALID_CONFIG_MESSAGE + config, ex);
        }
        return config;
    }

    public static String removeShortcutGroups(String config, String groupTitleKeys) {
        try {
            Object root = parse(config);
            if (root instanceof JSONArray) {
                return removeEntries((JSONArray) root, GROUP_TITLE_KEY, split(groupTitleKeys)).toString();
            }
        } catch (JSONException ex) {
            Log.w(TAG, INVALID_CONFIG_MESSAGE + config, ex);
        }
        return config;
    }

    private static Object parse(String config) throws JSONException {
        return (config == null || config.isEmpty()) ? null : new JSONTokener(config).nextValue();
    }

    private static void removeFromItems(JSONObject owner, Set<String> removedTypes) throws JSONException {
        JSONArray items = owner.optJSONArray(ITEMS);
        if (items != null) {
            owner.put(ITEMS, removeEntries(items, TYPE, removedTypes));
        }
    }

    private static JSONArray removeEntries(JSONArray entries, String key, Set<String> removedValues)
            throws JSONException {
        JSONArray kept = new JSONArray();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null || !removedValues.contains(entry.optString(key))) {
                kept.put(entries.get(i));
            }
        }
        return kept;
    }

    private static Set<String> split(String values) {
        return (values == null || values.isEmpty()) ? new HashSet<String>()
                : new HashSet<>(Arrays.asList(values.split(",")));
    }

}
