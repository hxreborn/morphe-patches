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

import android.net.Uri;
import android.util.Log;
import hx.util.Logs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class PromotionRewriter {

    private static final String[] PROMOTION_TARGETS = { "/member", "/web", "/profile" };

    private PromotionRewriter() {
    }

    static String withoutFeedPromotions(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            JSONArray items = (data != null) ? data.optJSONArray("items") : null;
            if (items == null) {
                return body;
            }
            boolean changed = false;
            for (int i = items.length() - 1; i >= 0; i--) {
                changed |= prune(items, i);
            }
            return changed ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "home feed not rewritten: " + ex.getMessage());
            return body;
        }
    }

    static String withoutSkuPromotion(String body) {
        try {
            JSONObject root = new JSONObject(body);
            if (root.isNull("data")) {
                return body;
            }
            root.put("data", JSONObject.NULL);
            return root.toString();
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "sku promotion not rewritten: " + ex.getMessage());
            return body;
        }
    }

    private static boolean prune(JSONArray items, int index) {
        JSONObject item = items.optJSONObject(index);
        if (item == null) {
            return false;
        }
        JSONObject banner = item.optJSONObject("banner");
        JSONObject custom = item.optJSONObject("customData");
        int removed = removePromotions(banner, "banners") + removePromotions(custom, "items");
        boolean emptied = removed > 0 && length(banner, "banners") + length(custom, "items") == 0;
        if (emptied || isPromotion(item.optJSONObject("page"))) {
            items.remove(index);
            return true;
        }
        return removed > 0;
    }

    private static int removePromotions(JSONObject holder, String listKey) {
        JSONArray list = (holder != null) ? holder.optJSONArray(listKey) : null;
        if (list == null) {
            return 0;
        }
        int removed = 0;
        for (int i = list.length() - 1; i >= 0; i--) {
            if (isPromotion(list.optJSONObject(i))) {
                list.remove(i);
                removed++;
            }
        }
        return removed;
    }

    private static int length(JSONObject holder, String listKey) {
        JSONArray list = (holder != null) ? holder.optJSONArray(listKey) : null;
        return (list != null) ? list.length() : 0;
    }

    private static boolean isPromotion(JSONObject entry) {
        return entry != null && leadsToPromotion(entry.optString("deepLink"));
    }

    private static boolean leadsToPromotion(String deepLink) {
        Uri uri = Uri.parse(deepLink);
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            return true;
        }
        String target = uri.getQueryParameter("type");
        for (String promotion : PROMOTION_TARGETS) {
            if (target != null && target.startsWith(promotion)) {
                return true;
            }
        }
        return false;
    }

}
