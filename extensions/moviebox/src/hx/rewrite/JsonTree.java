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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class JsonTree {

    private JsonTree() {
    }

    static boolean rewriteObjects(JSONObject node, JsonObjectRewrite rewrite) throws JSONException {
        boolean changed = rewrite.apply(node);
        for (String key : keysOf(node)) {
            changed |= rewriteChild(node.opt(key), rewrite);
        }
        return changed;
    }

    private static boolean rewriteArray(JSONArray array, JsonObjectRewrite rewrite) throws JSONException {
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            changed |= rewriteChild(array.opt(i), rewrite);
        }
        return changed;
    }

    private static boolean rewriteChild(Object value, JsonObjectRewrite rewrite) throws JSONException {
        if (value instanceof JSONObject) {
            return rewriteObjects((JSONObject) value, rewrite);
        }
        if (value instanceof JSONArray) {
            return rewriteArray((JSONArray) value, rewrite);
        }
        return false;
    }

    private static List<String> keysOf(JSONObject node) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = node.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        return keys;
    }

}
