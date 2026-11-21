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

import java.nio.charset.StandardCharsets;

import android.net.Uri;
import android.util.Log;
import hx.gateway.BuildTag;
import hx.gateway.CloudFrontPlayInfoClient;
import hx.gateway.PlayInfoLookup;
import hx.source.SignedResource;
import hx.util.Logs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class TvPlayInfo {

    private static final String PLAY_INFO_PATH = "/wefeed-tv-bff/subject/play-info/v2";

    private static final String MOBILE_PLAY_INFO_PATH = "/wefeed-mobile-bff/subject-api/play-info/v2";

    private static final int MOBILE_PLAYBACK_VERSION = 50020132;

    private TvPlayInfo() {
    }

    public static boolean isPlayInfo(String url) {
        return PLAY_INFO_PATH.equals(Uri.parse(url).getPath());
    }

    public static byte[] rewrite(String url, byte[] body) {
        return rewrite(url, body, Client.INSTANCE);
    }

    static byte[] rewrite(String url, byte[] body, PlayInfoLookup lookup) {
        Uri request = Uri.parse(url);
        String subjectId = request.getQueryParameter("subjectId");
        String season = request.getQueryParameter("se");
        String episode = request.getQueryParameter("ep");
        if (!PLAY_INFO_PATH.equals(request.getPath()) || subjectId == null || season == null || episode == null) {
            return body;
        }
        String path = new Uri.Builder().path(MOBILE_PLAY_INFO_PATH)
            .appendQueryParameter("subjectId", subjectId)
            .appendQueryParameter("se", season)
            .appendQueryParameter("ep", episode)
            .build()
            .toString();
        String replacement = lookup.fetchPlayInfo(path, (answer) -> SignedResource.fromPlayInfo(answer) != null);
        if (replacement == null) {
            return body;
        }
        try {
            JSONObject original = new JSONObject(new String(body, StandardCharsets.UTF_8));
            JSONObject data = original.getJSONObject("data");
            JSONArray streams = new JSONObject(replacement).getJSONObject("data").getJSONArray("streams");
            JSONArray signedStreams = new JSONArray();
            for (int i = 0; i < streams.length(); i++) {
                JSONObject stream = streams.getJSONObject(i);
                SignedResource resource = SignedResource.fromCookie(stream.optString("signCookie"));
                if (resource != null) {
                    stream.put("url", resource.manifestUrl).put("format", "DASH");
                    signedStreams.put(stream);
                }
            }
            if (signedStreams.length() == 0) {
                return body;
            }
            data.put("streams", signedStreams).put("resources", new JSONArray());
            Log.i(Logs.TAG, "TV playback uses signed DASH streams");
            return original.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "TV playback response is invalid", ex);
            return body;
        }
    }

    private static final class Client {

        private static final PlayInfoLookup INSTANCE = new CloudFrontPlayInfoClient(BuildTag.fromInstalledApp(),
                MOBILE_PLAYBACK_VERSION);

    }

}
