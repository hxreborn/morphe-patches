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
package hx.source;

import java.nio.charset.StandardCharsets;

import android.net.Uri;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class SignedResource {

    private static final String POLICY_KEY = "CloudFront-Policy=";

    private static final String EDGE_KEY = "Edge-Cache-Cookie=";

    public final String manifestUrl;

    final String cookie;

    private SignedResource(String cookie, String manifestUrl) {
        this.cookie = cookie;
        this.manifestUrl = manifestUrl;
    }

    public static SignedResource fromCookie(String cookie) {
        SignedResource cloudFront = fromCloudFront(cookie);
        return (cloudFront != null) ? cloudFront : fromEdgeCache(cookie);
    }

    public static SignedResource fromPlayInfo(String body) {
        try {
            return firstSigned(new JSONObject(body).getJSONObject("data").getJSONArray("streams"));
        } catch (JSONException ex) {
            return null;
        }
    }

    public static boolean isEdgeCachePlayInfo(String playInfo) {
        SignedResource resource = fromPlayInfo(playInfo);
        return resource != null && resource.isEdgeCache();
    }

    static String key(String subjectId, int season, int episode) {
        return subjectId + "/" + season + "/" + episode;
    }

    static String key(Uri playInfoRequest) {
        String subjectId = playInfoRequest.getQueryParameter("subjectId");
        String season = playInfoRequest.getQueryParameter("se");
        String episode = playInfoRequest.getQueryParameter("ep");
        if (subjectId == null || season == null || episode == null) {
            return null;
        }
        try {
            return key(subjectId, Integer.parseInt(season), Integer.parseInt(episode));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    boolean isEdgeCache() {
        return this.cookie.contains(EDGE_KEY);
    }

    private static SignedResource firstSigned(JSONArray streams) throws JSONException {
        for (int i = 0; i < streams.length(); i++) {
            SignedResource resource = fromCookie(streams.getJSONObject(i).optString("signCookie", ""));
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private static SignedResource fromCloudFront(String cookie) {
        String policy = valueAfter(cookie, POLICY_KEY, ';');
        if (policy == null) {
            return null;
        }
        try {
            String standard = policy.replace('-', '+').replace('_', '=').replace('~', '/');
            String json = new String(Base64.decode(standard, Base64.DEFAULT), StandardCharsets.UTF_8);
            String resource = new JSONObject(json).getJSONArray("Statement").getJSONObject(0).getString("Resource");
            if (!resource.startsWith("https://") || !resource.endsWith("/*")) {
                return null;
            }
            return new SignedResource(cookie, resource.substring(0, resource.length() - 2) + "/index.mpd");
        } catch (JSONException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static SignedResource fromEdgeCache(String cookie) {
        String value = valueAfter(cookie, EDGE_KEY, ';');
        if (value == null) {
            return null;
        }
        String prefixEncoded = valueAfter(value, "urlprefix=", ':');
        if (prefixEncoded == null) {
            return null;
        }
        try {
            String urlPrefix = new String(Base64.decode(prefixEncoded, Base64.URL_SAFE), StandardCharsets.UTF_8);
            if (!urlPrefix.startsWith("https://")) {
                return null;
            }
            String directory = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
            return new SignedResource(cookie, directory + "index.mpd");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String valueAfter(String source, String key, char delimiter) {
        int at = source.indexOf(key);
        if (at < 0) {
            return null;
        }
        int start = at + key.length();
        int end = source.indexOf(delimiter, start);
        return (end < 0) ? source.substring(start) : source.substring(start, end);
    }

}
