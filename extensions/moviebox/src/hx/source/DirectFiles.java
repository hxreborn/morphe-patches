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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.net.Uri;
import android.util.Log;
import hx.okhttp.ApiClient;
import hx.util.Logs;
import hx.util.RemoteFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class DirectFiles {

    private static final int RETENTION = 512;

    private static final long SIZE_LOOKUP_TIMEOUT_MS = 5000L;

    private final ApiClient api;

    private final Map<String, String> urls = new LruMap<>(RETENTION);

    private final Map<String, Long> sizes = new LruMap<>(RETENTION);

    private final ExecutorService sizeLookups = Executors.newCachedThreadPool();

    DirectFiles(ApiClient api) {
        this.api = api;
    }

    String knownUrl(String resourceId) {
        if (resourceId == null) {
            return null;
        }
        synchronized (this.urls) {
            return this.urls.get(resourceId);
        }
    }

    String resolveUrl(String subjectId, String resourceId, String origin) {
        String known = knownUrl(resourceId);
        if (known != null) {
            return known;
        }
        if (origin == null || resourceId == null) {
            return null;
        }
        String url;
        long length;
        try {
            url = fetchUrl(subjectId, resourceId, origin);
            length = url.isEmpty() ? -1 : RemoteFile.probeLength(url);
        } catch (IOException | JSONException | NumberFormatException ex) {
            Log.w(Logs.TAG, "direct file " + resourceId + " not checked", ex);
            return null;
        }
        Log.i(Logs.TAG, "direct file " + resourceId + ": "
                + (url.isEmpty() ? "none" : Uri.parse(url).getHost() + ", " + length + " bytes"));
        if (length <= 0 || url.equals(origin)) {
            return null;
        }
        remember(resourceId, url);
        return url;
    }

    String playInfoFor(String playInfo, String subjectId, int season, int episode) {
        if (subjectId == null || season < 0 || episode < 0) {
            return null;
        }
        String key = SignedResource.key(subjectId, season, episode);
        try {
            JSONObject root = new JSONObject(playInfo);
            JSONObject data = root.getJSONObject("data");
            List<JSONObject> streams = directStreams(subjectId, fetchEpisodeRows(subjectId, season, episode), key);
            if (streams.isEmpty()) {
                Log.w(Logs.TAG, "play-info " + key + ": no direct file");
                return null;
            }
            Collections.sort(streams, (a, b) -> Integer.compare(a.optInt("resolutions"), b.optInt("resolutions")));
            String resolutions = joinResolutions(streams);
            data.put("streams", new JSONArray(streams));
            data.put("displayResolutions", resolutions);
            Log.i(Logs.TAG, "play-info " + key + ": direct files at " + resolutions + "p on "
                    + Uri.parse(streams.get(0).getString("url")).getHost());
            return root.toString();
        } catch (IOException | JSONException ex) {
            Log.w(Logs.TAG, "play-info " + key + ": direct files not resolved", ex);
            return null;
        }
    }

    String withSizes(String resourceList) {
        try {
            JSONObject root = new JSONObject(resourceList);
            JSONObject data = root.optJSONObject("data");
            String subjectId = (data != null) ? data.optString("subjectId", "") : "";
            JSONArray list = (data != null) ? data.optJSONArray("list") : null;
            if (subjectId.isEmpty() || list == null) {
                return resourceList;
            }
            List<Callable<Boolean>> lookups = sizeLookupsFor(subjectId, list);
            int sized = countSized(subjectId,
                    this.sizeLookups.invokeAll(lookups, SIZE_LOOKUP_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Log.i(Logs.TAG, "resource list " + subjectId + ": sized " + sized + " of " + lookups.size() + " items");
            return (sized > 0) ? root.toString() : resourceList;
        } catch (JSONException | InterruptedException ex) {
            Log.w(Logs.TAG, "resource list sizes not read: " + ex.getMessage());
            return resourceList;
        }
    }

    private List<JSONObject> directStreams(String subjectId, List<JSONObject> rows, String key) {
        List<JSONObject> streams = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                JSONObject row = rows.get(i);
                String resourceId = row.getString("resourceId");
                String url = fetchUrl(subjectId, resourceId, row.optString("resourceLink"));
                if (!url.isEmpty()) {
                    streams.add(streamOf(row, resourceId, url));
                }
            } catch (IOException | JSONException ex) {
                Log.w(Logs.TAG, "play-info " + key + ": direct file row " + i + " not resolved", ex);
            }
        }
        return streams;
    }

    private static JSONObject streamOf(JSONObject row, String resourceId, String url) throws JSONException {
        return new JSONObject().put("format", "MP4")
            .put("id", resourceId)
            .put("url", url)
            .put("resolutions", String.valueOf(row.getInt("resolution")))
            .put("size", row.optString("size"))
            .put("duration", row.optInt("duration"))
            .put("codecName", row.optString("codecName"))
            .put("signCookie", "")
            .put("idType", "");
    }

    private static String joinResolutions(List<JSONObject> streams) throws JSONException {
        StringBuilder resolutions = new StringBuilder();
        for (JSONObject stream : streams) {
            if (resolutions.length() > 0) {
                resolutions.append(',');
            }
            resolutions.append(stream.getString("resolutions"));
        }
        return resolutions.toString();
    }

    private List<JSONObject> fetchEpisodeRows(String subjectId, int season, int episode)
            throws IOException, JSONException {
        JSONObject listing = fetchResourceListing(subjectId, season, episode, 0);
        List<JSONObject> rows = new ArrayList<>();
        addEpisodeRows(rows, listing.getJSONArray("list"), season, episode, 0);
        JSONArray collectionResolutions = listing.optJSONArray("collectionResolutions");
        if (collectionResolutions == null) {
            return rows;
        }
        for (int i = 0; i < collectionResolutions.length(); i++) {
            int resolution = collectionResolutions.getJSONObject(i).optInt("resolution");
            if (resolution > 0 && !hasResolution(rows, resolution)) {
                addResolutionRows(rows, subjectId, season, episode, resolution);
            }
        }
        return rows;
    }

    private void addResolutionRows(List<JSONObject> rows, String subjectId, int season, int episode, int resolution) {
        try {
            JSONArray list = fetchResourceListing(subjectId, season, episode, resolution).getJSONArray("list");
            addEpisodeRows(rows, list, season, episode, resolution);
        } catch (IOException | JSONException ex) {
            Log.w(Logs.TAG, "resource list " + SignedResource.key(subjectId, season, episode) + " at " + resolution
                    + "p not read", ex);
        }
    }

    private JSONObject fetchResourceListing(String subjectId, int season, int episode, int resolution)
            throws IOException, JSONException {
        String query = "?subjectId=" + subjectId + "&page=1&perPage=8&all=0&startPosition=1&endPosition=1"
                + "&pagerMode=2&resolution=" + resolution + "&se=" + season + "&epFrom=" + episode + "&epTo=" + episode
                + "&isVip=true";
        return new JSONObject(this.api.get(this.api.url(Endpoints.RESOURCE_LIST + query))).getJSONObject("data");
    }

    private static void addEpisodeRows(List<JSONObject> rows, JSONArray list, int season, int episode, int resolution)
            throws JSONException {
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.getJSONObject(i);
            boolean sameEpisode = row.optInt("se", -1) == season && row.optInt("ep", -1) == episode;
            if (sameEpisode && (resolution <= 0 || row.optInt("resolution") == resolution)) {
                rows.add(row);
            }
        }
    }

    private static boolean hasResolution(List<JSONObject> rows, int resolution) {
        for (JSONObject row : rows) {
            if (row.optInt("resolution") == resolution) {
                return true;
            }
        }
        return false;
    }

    private String fetchUrl(String subjectId, String resourceId, String origin) throws IOException, JSONException {
        JSONObject directFile = fetchFile(subjectId, resourceId, origin);
        return (directFile != null) ? directFile.optString("resourceLink", "") : "";
    }

    private JSONObject fetchFile(String subjectId, String resourceId, String origin) throws IOException, JSONException {
        String query = "?subjectId=" + subjectId + "&resourceId=" + resourceId + "&failUrl=" + Uri.encode(origin)
                + "&failCode=404&resourceNum=0&isVip=true";
        JSONArray list = new JSONObject(this.api.get(this.api.url(Endpoints.RESOURCE_POSITION + query)))
            .getJSONObject("data")
            .optJSONArray("list");
        return (list == null || list.length() == 0) ? null : list.getJSONObject(0);
    }

    private List<Callable<Boolean>> sizeLookupsFor(String subjectId, JSONArray list) {
        List<Callable<Boolean>> lookups = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item != null && !item.optString("resourceId", "").isEmpty()) {
                lookups.add(() -> resolveSize(subjectId, item));
            }
        }
        return lookups;
    }

    private static int countSized(String subjectId, List<Future<Boolean>> results) throws InterruptedException {
        int sized = 0;
        for (Future<Boolean> result : results) {
            try {
                if (!result.isCancelled() && result.get()) {
                    sized++;
                }
            } catch (ExecutionException ex) {
                Log.w(Logs.TAG, "resource list " + subjectId + ": direct file size not read", ex.getCause());
            }
        }
        return sized;
    }

    private boolean resolveSize(String subjectId, JSONObject item) throws IOException, JSONException {
        String resourceId = item.getString("resourceId");
        Long size = (knownUrl(resourceId) != null) ? cachedSize(resourceId) : null;
        if (size == null) {
            size = lookUpSize(subjectId, resourceId, item.optString("resourceLink", ""));
            if (size == null) {
                return false;
            }
        }
        item.put("size", String.valueOf(size));
        return true;
    }

    private Long cachedSize(String resourceId) {
        synchronized (this.sizes) {
            return this.sizes.get(resourceId);
        }
    }

    private Long lookUpSize(String subjectId, String resourceId, String origin) throws IOException, JSONException {
        JSONObject directFile = fetchFile(subjectId, resourceId, origin);
        String url = (directFile != null) ? directFile.optString("resourceLink", "") : "";
        long length = (directFile != null) ? directFile.optLong("size", 0) : 0;
        if (url.isEmpty() || url.equals(origin) || length <= 0) {
            return null;
        }
        synchronized (this.sizes) {
            this.sizes.put(resourceId, length);
        }
        remember(resourceId, url);
        return length;
    }

    private void remember(String resourceId, String url) {
        synchronized (this.urls) {
            this.urls.put(resourceId, url);
        }
    }

}
