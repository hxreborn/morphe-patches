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

    private static final String BANNER_SECTION = "BANNER";

    private static final String CUSTOM_SECTION = "CUSTOM";

    private static final String SINGLE_IMAGE_SECTION = "SINGLE_IMAGE";

    private static final String TARGET_PARAMETER = "type";

    private static final String[] PROMOTION_TARGETS = { "/member", "/web", "/profile" };

    private PromotionRewriter() {
    }

    static String withoutFeedPromotions(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONArray sections = root.getJSONObject("data").getJSONArray("items");
            boolean changed = false;
            for (int i = sections.length() - 1; i >= 0; i--) {
                changed |= removePromotions(sections, i);
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

    private static boolean removePromotions(JSONArray sections, int index) throws JSONException {
        JSONObject section = sections.getJSONObject(index);
        switch (section.optString("type")) {
            case BANNER_SECTION:
                return removePromotionEntries(sections, index, section.getJSONObject("banner").getJSONArray("banners"));
            case CUSTOM_SECTION:
                return removePromotionEntries(sections, index,
                        section.getJSONObject("customData").getJSONArray("items"));
            case SINGLE_IMAGE_SECTION:
                return removePromotionImage(sections, index, section.getJSONObject("page"));
            default:
                return false;
        }
    }

    private static boolean removePromotionEntries(JSONArray sections, int index, JSONArray entries)
            throws JSONException {
        boolean removed = false;
        for (int i = entries.length() - 1; i >= 0; i--) {
            if (isPromotion(entries.getJSONObject(i))) {
                entries.remove(i);
                removed = true;
            }
        }
        if (removed && entries.length() == 0) {
            sections.remove(index);
        }
        return removed;
    }

    private static boolean removePromotionImage(JSONArray sections, int index, JSONObject page) {
        boolean promotion = isPromotion(page);
        if (promotion) {
            sections.remove(index);
        }
        return promotion;
    }

    private static boolean isPromotion(JSONObject entry) {
        Uri deepLink = Uri.parse(entry.optString("deepLink"));
        return isWebLink(deepLink) || (deepLink.isHierarchical() && isPromotionTarget(deepLink));
    }

    private static boolean isWebLink(Uri deepLink) {
        return "http".equals(deepLink.getScheme()) || "https".equals(deepLink.getScheme());
    }

    private static boolean isPromotionTarget(Uri deepLink) {
        String target = deepLink.getQueryParameter(TARGET_PARAMETER);
        if (target == null) {
            return false;
        }
        for (String promotion : PROMOTION_TARGETS) {
            if (startsAtBoundary(target, promotion)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsAtBoundary(String target, String promotion) {
        int end = promotion.length();
        return target.startsWith(promotion)
                && (target.length() == end || target.charAt(end) == '/' || target.charAt(end) == '?');
    }

}
