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

import android.util.Log;
import hx.loopback.DashServer;
import hx.util.Logs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ResourceListRewriter {

    private ResourceListRewriter() {
    }

    static String rewrite(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            String subjectId = (data != null) ? data.optString("subjectId", "") : "";
            JSONArray list = (data != null) ? data.optJSONArray("list") : null;
            if (subjectId.isEmpty() || list == null) {
                return body;
            }
            int routed = 0;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item != null && route(subjectId, item)) {
                    routed++;
                }
            }
            Log.i(Logs.TAG, "resource list " + subjectId + ": routed " + routed + " of " + list.length() + " items");
            return (routed > 0) ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "resource list not rewritten: " + ex.getMessage());
            return body;
        }
    }

    private static boolean route(String subjectId, JSONObject item) throws JSONException {
        if (!item.has("resourceLink")) {
            return false;
        }
        int season;
        int episode;
        int height;
        String origin;
        try {
            season = item.getInt("se");
            episode = item.getInt("ep");
            height = item.getInt("resolution");
            origin = item.getString("resourceLink");
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "resource item " + item.optString("resourceId") + " not routed: " + ex.getMessage());
            return false;
        }
        if (season < 0 || episode < 0 || height <= 0) {
            Log.w(Logs.TAG, "resource item " + item.optString("resourceId") + " kept as is: se=" + season + " ep="
                    + episode + " resolution=" + height);
            return false;
        }
        String resourceId = item.optString("resourceId", "");
        item.put("resourceLink", DashServer.buildUrl(subjectId, season, episode, height, origin,
                item.optLong("size", 0), resourceId.isEmpty() ? null : resourceId));
        return true;
    }

}
