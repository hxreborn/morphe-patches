/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
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

    static boolean rewriteObjects(JSONObject node, ObjectRewrite rewrite) throws JSONException {
        boolean changed = rewrite.apply(node);
        for (String key : keysOf(node)) {
            changed |= rewriteChild(node.opt(key), rewrite);
        }
        return changed;
    }

    private static boolean rewriteArray(JSONArray array, ObjectRewrite rewrite) throws JSONException {
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            changed |= rewriteChild(array.opt(i), rewrite);
        }
        return changed;
    }

    private static boolean rewriteChild(Object value, ObjectRewrite rewrite) throws JSONException {
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

    interface ObjectRewrite {

        boolean apply(JSONObject node) throws JSONException;

    }

}
