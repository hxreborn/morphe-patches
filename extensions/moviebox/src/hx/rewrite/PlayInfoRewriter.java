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

import android.util.Log;
import hx.loopback.DashServer;
import hx.source.SignedResource;
import hx.util.Logs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class PlayInfoRewriter {

    private static final String[] URL_FIELDS = { "url", "resourceLink", "downloadUrl", "playUrl" };

    private PlayInfoRewriter() {
    }

    static String pointAtManifests(String body) {
        return rewrite(body, null, -1, -1);
    }

    static String rewrite(String body, String subjectId, int season, int episode) {
        try {
            JSONObject root = new JSONObject(body);
            boolean changed = JsonTree.rewriteObjects(root, PlayInfoRewriter::pointAtManifest);
            changed |= routeProgressiveStreams(root, subjectId, season, episode);
            return changed ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "play-info not rewritten: " + ex.getMessage());
            return body;
        }
    }

    private static boolean pointAtManifest(JSONObject node) throws JSONException {
        SignedResource resource = SignedResource.fromCookie(node.optString("signCookie", ""));
        if (resource == null) {
            return false;
        }
        boolean changed = false;
        for (String field : URL_FIELDS) {
            if (node.has(field)) {
                node.put(field, resource.manifestUrl);
                changed = true;
            }
        }
        if (changed) {
            node.put("format", "DASH");
        }
        return changed;
    }

    private static boolean routeProgressiveStreams(JSONObject root, String subjectId, int season, int episode)
            throws JSONException {
        if (subjectId == null || season < 0 || episode < 0) {
            return false;
        }
        JSONObject data = root.optJSONObject("data");
        JSONArray streams = (data != null) ? data.optJSONArray("streams") : null;
        if (streams == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < streams.length(); i++) {
            JSONObject stream = streams.optJSONObject(i);
            if (stream != null && routeThroughLoopback(stream, subjectId, season, episode)) {
                changed = true;
            }
        }
        return changed;
    }

    private static boolean routeThroughLoopback(JSONObject stream, String subjectId, int season, int episode)
            throws JSONException {
        if (!stream.optString("signCookie", "").isEmpty()) {
            return false;
        }
        String origin = stream.optString("url", "");
        int height = highestResolution(stream.optString("resolutions", ""));
        if (origin.isEmpty() || height <= 0) {
            return false;
        }
        stream.put("url",
                DashServer.buildUrl(subjectId, season, episode, height, origin, stream.optLong("size", 0), null));
        return true;
    }

    private static int highestResolution(String resolutions) {
        int best = 0;
        for (String part : resolutions.split(",")) {
            best = Math.max(best, parseResolution(part));
        }
        return best;
    }

    private static int parseResolution(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
