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
package app.hxreborn.extension.bereal.backup;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.S_V2)
public final class MemoriesJson {

    private MemoriesJson() {
    }

    public static List<String> momentIds(final String v1Body) throws JSONException {
        final JSONArray data = new JSONObject(v1Body).optJSONArray("data");
        final List<String> ids = new ArrayList<>();
        for (int i = 0; data != null && i < data.length(); i++) {
            final JSONObject item = data.optJSONObject(i);
            final String id = (item != null) ? string(item, "momentId") : null;
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static List<MemoryPost> posts(final String momentId, final String v2Body)
            throws JSONException {
        final JSONArray array = new JSONObject(v2Body).optJSONArray("posts");
        final List<MemoryPost> posts = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            final JSONObject post = array.optJSONObject(i);
            if (post != null) {
                posts.add(post(momentId, post));
            }
        }
        return posts;
    }

    public static JSONObject sidecar(final MemoryPost post, final List<String> savedFiles)
            throws JSONException {
        final JSONObject json = new JSONObject();
        json.put("id", post.id);
        json.put("momentId", post.momentId);
        putTime(json, "takenAt", post.takenAt);
        putTime(json, "postedAt", post.postedAt);
        json.put("isMain", post.isMain);
        json.put("isLate", post.isLate);
        putIfPresent(json, "back", media(post.back));
        putIfPresent(json, "front", media(post.front));
        putIfPresent(json, "btsUrl", post.btsUrl);
        putIfPresent(json, "caption", post.caption);
        putIfPresent(json, "music", post.music);
        if (post.location != null) {
            json.put("location", new JSONObject()
                    .put("latitude", post.location.latitude)
                    .put("longitude", post.location.longitude));
        }
        json.put("visibility", new JSONArray(post.visibility));
        final JSONArray realmojis = new JSONArray();
        for (final MemoryPost.RealMoji r : post.realmojis) {
            final JSONObject o = new JSONObject()
                    .put("username", r.username)
                    .put("emoji", r.emoji);
            putIfPresent(o, "url", r.url);
            putTime(o, "postedAt", r.postedAt);
            realmojis.put(o);
        }
        json.put("realmojis", realmojis);
        final JSONArray comments = new JSONArray();
        for (final MemoryPost.Comment c : post.comments) {
            final JSONObject o = new JSONObject()
                    .put("username", c.username)
                    .put("text", c.text);
            putTime(o, "postedAt", c.postedAt);
            comments.put(o);
        }
        json.put("comments", comments);
        json.put("files", new JSONArray(savedFiles));
        return json;
    }

    private static MemoryPost post(final String momentId, final JSONObject post) {
        final List<String> visibility = new ArrayList<>();
        final JSONArray vis = post.optJSONArray("visibility");
        for (int i = 0; vis != null && i < vis.length(); i++) {
            final String v = vis.optString(i, "");
            if (!v.isEmpty()) {
                visibility.add(v);
            }
        }
        final List<MemoryPost.RealMoji> realmojis = new ArrayList<>();
        final JSONArray rms = post.optJSONArray("realmojis");
        for (int i = 0; rms != null && i < rms.length(); i++) {
            final JSONObject r = rms.optJSONObject(i);
            if (r != null) {
                final JSONObject media = r.optJSONObject("media");
                realmojis.add(new MemoryPost.RealMoji(
                        username(r),
                        orEmpty(string(r, "emoji")),
                        (media != null) ? string(media, "url") : null,
                        time(string(r, "postedAt"))));
            }
        }
        final List<MemoryPost.Comment> comments = new ArrayList<>();
        final JSONArray cms = post.optJSONArray("comments");
        for (int i = 0; cms != null && i < cms.length(); i++) {
            final JSONObject c = cms.optJSONObject(i);
            if (c != null) {
                comments.add(new MemoryPost.Comment(
                        username(c),
                        orEmpty(string(c, "content")),
                        time(string(c, "postedAt"))));
            }
        }
        final JSONObject loc = post.optJSONObject("location");
        final double lat = (loc != null) ? loc.optDouble("latitude") : Double.NaN;
        final double lon = (loc != null) ? loc.optDouble("longitude") : Double.NaN;
        // NaN from null or non-numeric coordinates can't be serialized
        final MemoryPost.Location location =
                (Double.isNaN(lat) || Double.isNaN(lon)) ? null : new MemoryPost.Location(lat, lon);
        return new MemoryPost(
                orEmpty(string(post, "id")),
                momentId,
                time(string(post, "takenAt")),
                time(string(post, "postedAt")),
                post.optBoolean("isMain"),
                post.optBoolean("isLate"),
                media(post.optJSONObject("primary")),
                media(post.optJSONObject("secondary")),
                bts(post.opt("btsMedia")),
                string(post, "caption"),
                location,
                music(post.optJSONObject("music")),
                visibility,
                realmojis,
                comments);
    }

    private static String music(final JSONObject music) {
        if (music == null) {
            return null;
        }
        final String artist = string(music, "artist");
        final String track = string(music, "track");
        if (artist != null && track != null) {
            return artist + " - " + track;
        }
        return (artist != null) ? artist : track;
    }

    private static String bts(final Object value) {
        if (value instanceof JSONObject) {
            return string((JSONObject) value, "url");
        }
        if (value instanceof String && !((String) value).isEmpty()) {
            return (String) value;
        }
        return null;
    }

    private static MemoryPost.Media media(final JSONObject json) {
        final String url = (json != null) ? string(json, "url") : null;
        return (url != null)
                ? new MemoryPost.Media(url, json.optInt("width"), json.optInt("height"))
                : null;
    }

    private static JSONObject media(final MemoryPost.Media media) throws JSONException {
        if (media == null) {
            return null;
        }
        return new JSONObject()
                .put("url", media.url)
                .put("width", media.width)
                .put("height", media.height);
    }

    private static void putTime(final JSONObject json, final String key, final long millis)
            throws JSONException {
        // 0 means the time was missing
        if (millis != 0) {
            json.put(key, iso(millis));
        }
    }

    private static void putIfPresent(final JSONObject json, final String key, final Object value)
            throws JSONException {
        if (value != null) {
            json.put(key, value);
        }
    }

    private static String username(final JSONObject json) {
        final JSONObject user = json.optJSONObject("user");
        return orEmpty((user != null) ? string(user, "username") : null);
    }

    private static String string(final JSONObject json, final String key) {
        if (json.isNull(key)) {
            return null;
        }
        final String value = json.optString(key, "");
        return (!value.isEmpty()) ? value : null;
    }

    private static String orEmpty(final String value) {
        return (value != null) ? value : "";
    }

    private static long time(final String iso) {
        if (iso == null) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (final DateTimeParseException ex) {
            return 0L;
        }
    }

    private static String iso(final long millis) {
        return Instant.ofEpochMilli(millis).toString();
    }
}
