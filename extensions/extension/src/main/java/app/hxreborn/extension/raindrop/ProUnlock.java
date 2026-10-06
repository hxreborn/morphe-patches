/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class ProUnlock {

    private static final String TAG = "RaindropPro";

    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;

    private static final int MAX_PAGE_TEXT_MATCHES = 200;

    private static final long FIRST_SCAN_TIMEOUT_MILLIS = 5000;

    private static final long NO_BOOKMARK_ID = 0;

    private static final String REFUSED_REQUEST_URL = RaindropApi.API_URL + "raindrops/0/hx-refused";

    private static final Pattern DUPLICATES_OF = Pattern.compile("_id:(\\d+) duplicate:\\1 match:OR");

    private static final Pattern ARCHIVE_PATH = Pattern.compile("raindrop/(\\d+)/cache");

    private static final Pattern RESPONSE_CODE = Pattern.compile("code=(\\d+)");

    private static final ThreadLocal<String> responseUrl = new ThreadLocal<>();

    private static final ThreadLocal<String> replacementRequestBody = new ThreadLocal<>();

    private static volatile Context applicationContext;

    private static volatile boolean hasNativePro;

    private static volatile boolean isBrokenCheckEnabled = true;

    private static volatile boolean isSuggestionsEnabled = true;

    private ProUnlock() {
    }

    public static void onResponse(Context context, Object response) {
        responseUrl.remove();
        try {
            if (context != null && applicationContext == null) {
                applicationContext = context.getApplicationContext();
                LinkChecker.init(applicationContext);
                BookmarkIndex.init(applicationContext);
            }
            String description = String.valueOf(response);
            Matcher code = RESPONSE_CODE.matcher(description);
            int start = description.indexOf("url=");
            int end = description.lastIndexOf('}');
            if (code.find() && "200".equals(code.group(1)) && start >= 0 && end > start) {
                responseUrl.set(description.substring(start + 4, end));
            }
        } catch (RuntimeException ex) {
            Log.w(TAG, "Response inspection failed", ex);
        }
    }

    public static String onRequest(String method, String url, int requestId, Object headers, Object bodyMap) {
        replacementRequestBody.remove();
        try {
            return rewriteBulkRequest(method, url, bodyMap);
        } catch (JSONException | ReflectiveOperationException | RuntimeException ex) {
            Log.w(TAG, "Request rewrite failed for " + method + " " + url, ex);
            return url;
        }
    }

    public static String replaceRequestBody(String body) {
        String replacement = replacementRequestBody.get();
        replacementRequestBody.remove();
        return (replacement != null) ? replacement : body;
    }

    public static String archiveUrl(String url) {
        try {
            if (hasNativePro || url == null || !url.startsWith(RaindropApi.API_URL)) {
                return url;
            }
            Matcher archive = ARCHIVE_PATH.matcher(url.substring(RaindropApi.API_URL.length()));
            if (!archive.matches()) {
                return url;
            }
            String link = BookmarkIndex.linkOf(Long.parseLong(archive.group(1)));
            return (link != null && LinkChecker.isArchived(link)) ? LinkChecker.archiveUrl(link) : url;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Archive lookup failed for " + url, ex);
            return url;
        }
    }

    public static Map<?, ?> archiveHeaders(String url, Map<?, ?> headers) {
        return (url != null && !url.equals(archiveUrl(url))) ? new HashMap<>() : headers;
    }

    public static byte[] rewriteResponse(byte[] body) {
        String url = responseUrl.get();
        responseUrl.remove();
        if (url == null || !url.startsWith(RaindropApi.API_URL) || body == null || body.length == 0
                || body.length > MAX_BODY_BYTES) {
            return body;
        }
        try {
            JSONObject response = new JSONObject(new String(body, StandardCharsets.UTF_8));
            JSONObject rewritten = rewrite(Uri.parse(url), response);
            return (rewritten != null) ? rewritten.toString().getBytes(StandardCharsets.UTF_8) : body;
        } catch (JSONException ex) {
            return body;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Rewrite failed for " + url, ex);
            return body;
        }
    }

    private static String rewriteBulkRequest(String method, String url, Object bodyMap)
            throws JSONException, ReflectiveOperationException {
        if (hasNativePro || !("PUT".equals(method) || "DELETE".equals(method)) || url == null
                || !url.startsWith(RaindropApi.API_URL)) {
            return url;
        }
        Uri uri = Uri.parse(url);
        List<String> path = uri.getPathSegments();
        if (path.size() != 3 || !"raindrops".equals(path.get(1))
                || !"true".equals(uri.getQueryParameter("dangerAll"))) {
            return url;
        }
        String query = searchOf(uri);
        boolean isLocal = isLocalQuery(query);
        if (!isLocal && !isPlainText(query)) {
            return url;
        }
        if (isLocal && !BookmarkIndex.isLoaded()) {
            return REFUSED_REQUEST_URL;
        }
        try {
            String body = stringBodyOf(bodyMap);
            if (body == null) {
                return isLocal ? REFUSED_REQUEST_URL : url;
            }
            JSONObject request = new JSONObject(body);
            if (!request.has("ids")) {
                if (!isLocal) {
                    return url;
                }
                JSONArray ids = new JSONArray();
                for (JSONObject bookmark : localResults(collectionId(path.get(2)), query)) {
                    ids.put(bookmark.optLong("_id"));
                }
                if (ids.length() == 0) {
                    ids.put(NO_BOOKMARK_ID);
                }
                replacementRequestBody.set(request.put("ids", ids).toString());
            }
            return withoutSearch(uri);
        } catch (JSONException | ReflectiveOperationException | RuntimeException ex) {
            if (isLocal) {
                Log.w(TAG, "Refused " + method + " " + url, ex);
                replacementRequestBody.remove();
                return REFUSED_REQUEST_URL;
            }
            throw ex;
        }
    }

    private static String stringBodyOf(Object bodyMap) throws ReflectiveOperationException {
        if (bodyMap == null
                || !(Boolean) bodyMap.getClass().getMethod("hasKey", String.class).invoke(bodyMap, "string")) {
            return null;
        }
        return (String) bodyMap.getClass().getMethod("getString", String.class).invoke(bodyMap, "string");
    }

    private static String withoutSearch(Uri uri) {
        Uri.Builder builder = uri.buildUpon().clearQuery();
        for (String name : uri.getQueryParameterNames()) {
            if ("search".equals(name)) {
                continue;
            }
            for (String value : uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value);
            }
        }
        return builder.build().toString();
    }

    private static JSONObject rewrite(Uri url, JSONObject response) throws JSONException {
        List<String> path = url.getPathSegments();
        if (path.size() < 2) {
            return null;
        }
        String resource = path.get(1);
        if ("user".equals(resource)) {
            return (path.size() == 2) ? unlockUser(response) : null;
        }
        if (hasNativePro) {
            return null;
        }
        boolean isSuccess = response.optBoolean("result");
        switch (resource) {
            case "raindrop":
                return rewriteBookmark(path, response);
            case "raindrops":
                if (path.size() != 3 || !isSuccess) {
                    return null;
                }
                return rewriteBookmarks(url, collectionId(path.get(2)), response);
            case "filters":
                if (path.size() != 3 || !isSuccess) {
                    return null;
                }
                return addFilterCounts(collectionId(path.get(2)), response);
            case "collection":
                if (path.size() != 4 || !"lastAction".equals(path.get(3)) || !isSuccess) {
                    return null;
                }
                return stampVersion(response);
            default:
                return null;
        }
    }

    private static JSONObject rewriteBookmark(List<String> path, JSONObject response) throws JSONException {
        if (path.size() == 4 && "suggest".equals(path.get(3))) {
            return suggest(path.get(2), response);
        }
        JSONObject item = response.optJSONObject("item");
        if (path.size() > 3 || !response.optBoolean("result") || item == null) {
            return null;
        }
        BookmarkIndex.refreshInBackground();
        List<JSONObject> observed = new ArrayList<>();
        observed.add(item);
        BookmarkIndex.observe(observed);
        addLocalFlags(item);
        return response;
    }

    private static JSONObject unlockUser(JSONObject response) throws JSONException {
        JSONObject user = response.optJSONObject("user");
        if (!response.optBoolean("result") || user == null || !user.has("pro")) {
            return null;
        }
        hasNativePro = user.optBoolean("pro");
        if (hasNativePro) {
            return null;
        }
        JSONObject config = user.optJSONObject("config");
        isBrokenCheckEnabled = config == null || !"off".equals(config.optString("broken_level"));
        isSuggestionsEnabled = config == null || config.optBoolean("ai_suggestions", true);
        long account = user.optLong("_id");
        BookmarkIndex.useAccount(account);
        BookmarkIndex.refreshInBackground();
        Context context = applicationContext;
        if (context != null && account != 0) {
            Backups.backUpIfDue(context, account);
        }
        user.put("pro", true);
        return response;
    }

    private static JSONObject stampVersion(JSONObject response) throws JSONException {
        if (!response.has("version")) {
            return null;
        }
        BookmarkIndex.awaitFirstScan(FIRST_SCAN_TIMEOUT_MILLIS);
        String stamp = BookmarkIndex.isLoaded() ? LinkChecker.resultStamp() + "." + BookmarkIndex.duplicateCount()
                : "loading";
        return response.put("version", response.optString("version") + "+hx." + stamp);
    }

    private static JSONObject addFilterCounts(int collectionId, JSONObject response) throws JSONException {
        if (collectionId == BookmarkIndex.TRASH) {
            return null;
        }
        BookmarkIndex.awaitFirstScan(FIRST_SCAN_TIMEOUT_MILLIS);
        if (!BookmarkIndex.isLoaded()) {
            return null;
        }
        int duplicates = 0;
        int broken = 0;
        for (JSONObject bookmark : BookmarkIndex.snapshot()) {
            if (!BookmarkIndex.inCollection(bookmark, collectionId)) {
                continue;
            }
            if (BookmarkIndex.originalOf(bookmark.optLong("_id")) != 0) {
                duplicates++;
            }
            if (isBroken(bookmark.optString("link"))) {
                broken++;
            }
        }
        if (duplicates > 0) {
            response.put("duplicate", new JSONObject().put("count", duplicates));
        }
        if (broken > 0) {
            response.put("broken", new JSONObject().put("count", broken));
        }
        return response;
    }

    private static JSONObject rewriteBookmarks(Uri url, int collectionId, JSONObject response) throws JSONException {
        JSONArray items = response.optJSONArray("items");
        if (items == null) {
            return null;
        }
        String query = searchOf(url);
        if (isLocalQuery(query)) {
            BookmarkIndex.awaitFirstScan(FIRST_SCAN_TIMEOUT_MILLIS);
            if (!BookmarkIndex.isLoaded()) {
                return new JSONObject().put("result", false).put("errorMessage", "Bookmark index is still loading");
            }
            return page(url, collectionId, localResults(collectionId, query));
        }
        BookmarkIndex.refreshInBackground();
        Set<Long> listed = new HashSet<>();
        List<JSONObject> observed = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            observed.add(item);
            listed.add(item.optLong("_id"));
        }
        BookmarkIndex.observe(observed);
        for (JSONObject item : observed) {
            addLocalFlags(item);
        }
        if (isPlainText(query) && intParameter(url, "page", 0) == 0 && response.optInt("count") <= items.length()) {
            addPageTextMatches(query, collectionId, listed, items, response);
        }
        return response;
    }

    private static String searchOf(Uri url) {
        String search = url.getQueryParameter("search");
        return (search != null) ? search.trim() : "";
    }

    private static boolean isLocalQuery(String query) {
        return "duplicate:true".equals(query) || "broken:true".equals(query) || DUPLICATES_OF.matcher(query).matches();
    }

    private static List<JSONObject> localResults(int collectionId, String query) {
        if ("duplicate:true".equals(query)) {
            return duplicatesIn(collectionId);
        }
        if ("broken:true".equals(query)) {
            return brokenIn(collectionId);
        }
        Matcher duplicatesOf = DUPLICATES_OF.matcher(query);
        if (duplicatesOf.matches()) {
            return duplicatesOf(Long.parseLong(duplicatesOf.group(1)));
        }
        return new ArrayList<>();
    }

    private static boolean isPlainText(String query) {
        if (query.isEmpty() || query.indexOf(':') >= 0 || query.indexOf('#') >= 0 || query.indexOf('"') >= 0) {
            return false;
        }
        for (String word : query.split("\\s+")) {
            if (word.startsWith("-")) {
                return false;
            }
        }
        return true;
    }

    private static void addPageTextMatches(String query, int collectionId, Set<Long> listed, JSONArray items,
            JSONObject response) throws JSONException {
        Set<String> links = LinkChecker.linksContaining(query);
        if (links.isEmpty()) {
            return;
        }
        int added = 0;
        for (JSONObject bookmark : BookmarkIndex.snapshot()) {
            if (added == MAX_PAGE_TEXT_MATCHES) {
                break;
            }
            if (BookmarkIndex.inCollection(bookmark, collectionId) && links.contains(bookmark.optString("link"))
                    && listed.add(bookmark.optLong("_id"))) {
                JSONObject item = new JSONObject(bookmark.toString());
                addLocalFlags(item);
                items.put(item);
                added++;
            }
        }
        response.put("count", response.optInt("count") + added);
    }

    private static List<JSONObject> duplicatesIn(int collectionId) {
        List<JSONObject> matches = new ArrayList<>();
        for (JSONObject bookmark : BookmarkIndex.snapshot()) {
            if (BookmarkIndex.inCollection(bookmark, collectionId)
                    && BookmarkIndex.originalOf(bookmark.optLong("_id")) != 0) {
                matches.add(bookmark);
            }
        }
        return matches;
    }

    private static List<JSONObject> brokenIn(int collectionId) {
        List<JSONObject> matches = new ArrayList<>();
        for (JSONObject bookmark : BookmarkIndex.snapshot()) {
            if (BookmarkIndex.inCollection(bookmark, collectionId) && isBroken(bookmark.optString("link"))) {
                matches.add(bookmark);
            }
        }
        return matches;
    }

    private static List<JSONObject> duplicatesOf(long originalId) {
        List<JSONObject> matches = new ArrayList<>();
        for (JSONObject bookmark : BookmarkIndex.snapshot()) {
            long id = bookmark.optLong("_id");
            if (id == originalId || BookmarkIndex.originalOf(id) == originalId) {
                matches.add(bookmark);
            }
        }
        return matches;
    }

    private static JSONObject page(Uri url, int collectionId, List<JSONObject> matches) throws JSONException {
        int perPage = Math.max(1, intParameter(url, "perpage", 25));
        long offset = (long) Math.max(0, intParameter(url, "page", 0)) * perPage;
        int first = (int) Math.min(offset, matches.size());
        int end = (int) Math.min(offset + perPage, matches.size());
        JSONArray items = new JSONArray();
        for (int i = first; i < end; i++) {
            JSONObject item = new JSONObject(matches.get(i).toString());
            addLocalFlags(item);
            items.put(item);
        }
        return new JSONObject().put("result", true)
            .put("items", items)
            .put("count", matches.size())
            .put("collectionId", collectionId);
    }

    private static JSONObject suggest(String bookmarkId, JSONObject response) throws JSONException {
        if (!isSuggestionsEnabled || response.optBoolean("result", true)) {
            return null;
        }
        JSONObject bookmark = BookmarkIndex.find(Long.parseLong(bookmarkId));
        return (bookmark != null) ? Suggestions.forBookmark(bookmark, BookmarkIndex.snapshot()) : null;
    }

    private static boolean isBroken(String link) {
        return isBrokenCheckEnabled && LinkChecker.isBroken(link);
    }

    private static void addLocalFlags(JSONObject item) throws JSONException {
        long original = BookmarkIndex.originalOf(item.optLong("_id"));
        if (original != 0) {
            item.put("duplicate", original);
        }
        String link = item.optString("link");
        if (isBroken(link)) {
            item.put("broken", true);
        }
        if (LinkChecker.isArchived(link)) {
            item.put("cache", new JSONObject().put("status", "ready"));
        }
    }

    private static int collectionId(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException ex) {
            return BookmarkIndex.ALL;
        }
    }

    private static int intParameter(Uri url, String name, int fallback) {
        String value = url.getQueryParameter(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

}
