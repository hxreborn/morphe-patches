/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class BookmarkIndex {

    static final int ALL = 0;

    static final int UNSORTED = -1;

    static final int TRASH = -99;

    private static final String TAG = "RaindropPro";

    private static final int PAGE_SIZE = 50;

    private static final int MAX_PAGES = 100;

    private static final long MAX_AGE_MILLIS = 10 * 60 * 1000;

    private static final long COUNT_CHECK_INTERVAL_MILLIS = 60 * 1000;

    private static final ExecutorService scanner = Executors.newSingleThreadExecutor();

    private static final AtomicBoolean refreshQueued = new AtomicBoolean();

    private static final Map<Long, JSONObject> bookmarks = new LinkedHashMap<>();

    private static final Map<Long, Long> originals = new HashMap<>();

    private static final Map<Long, String> links = new ConcurrentHashMap<>();

    private static File savedIndex;

    private static long account;

    private static long loadedAt;

    private static long countCheckedAt;

    private static boolean loaded;

    private static boolean complete;

    private static boolean hasAwaitedFirstScan;

    private static boolean hasScanned;

    private BookmarkIndex() {
    }

    static synchronized void init(Context context) {
        if (savedIndex != null) {
            return;
        }
        savedIndex = new File(context.getFilesDir(), "hx_raindrop_index.json");
        if (!savedIndex.exists()) {
            return;
        }
        try (InputStream input = new FileInputStream(savedIndex)) {
            byte[] bytes = new byte[(int) savedIndex.length()];
            int read = 0;
            while (read < bytes.length) {
                int count = input.read(bytes, read, bytes.length - read);
                if (count < 0) {
                    throw new IOException("Unexpected end of " + savedIndex);
                }
                read += count;
            }
            JSONObject saved = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            JSONArray items = saved.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                bookmarks.put(item.getLong("_id"), item);
                links.put(item.getLong("_id"), item.optString("link"));
            }
            account = saved.getLong("account");
            LinkChecker.useAccount(account);
            loaded = true;
            loadedAt = SystemClock.elapsedRealtime() - MAX_AGE_MILLIS;
            findDuplicates();
        } catch (IOException | JSONException ex) {
            Log.w(TAG, "Saved bookmark index unreadable", ex);
            bookmarks.clear();
            links.clear();
        }
    }

    static synchronized void useAccount(long id) {
        LinkChecker.useAccount(id);
        if (id == account) {
            return;
        }
        account = id;
        loaded = false;
        complete = false;
        hasAwaitedFirstScan = false;
        hasScanned = false;
        loadedAt = 0;
        countCheckedAt = 0;
        bookmarks.clear();
        originals.clear();
        links.clear();
    }

    static void refreshInBackground() {
        if (refreshQueued.compareAndSet(false, true)) {
            scanner.execute(() -> {
                try {
                    refreshIfStale();
                } finally {
                    refreshQueued.set(false);
                }
            });
        }
    }

    static synchronized void awaitFirstScan(long timeoutMillis) {
        refreshInBackground();
        if (hasAwaitedFirstScan) {
            return;
        }
        hasAwaitedFirstScan = true;
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
        try {
            while (!hasScanned) {
                long remaining = deadline - SystemClock.elapsedRealtime();
                if (remaining <= 0) {
                    return;
                }
                BookmarkIndex.class.wait(remaining);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    static synchronized boolean isLoaded() {
        return loaded;
    }

    static synchronized List<JSONObject> snapshot() {
        return new ArrayList<>(bookmarks.values());
    }

    static synchronized JSONObject find(long id) {
        return bookmarks.get(id);
    }

    static String linkOf(long id) {
        return links.get(id);
    }

    static synchronized int duplicateCount() {
        return originals.size();
    }

    static synchronized long originalOf(long id) {
        Long original = originals.get(id);
        return (original != null) ? original : 0;
    }

    static synchronized void observe(List<JSONObject> observed) {
        if (!loaded) {
            return;
        }
        List<JSONObject> changedLinks = new ArrayList<>();
        for (JSONObject bookmark : observed) {
            long id = bookmark.optLong("_id");
            if (id == 0 || !bookmark.has("link")) {
                continue;
            }
            if (bookmark.optInt("collectionId", ALL) == TRASH || bookmark.optBoolean("removed")) {
                bookmarks.remove(id);
                links.remove(id);
                continue;
            }
            JSONObject copy;
            try {
                copy = new JSONObject(bookmark.toString());
            } catch (JSONException ex) {
                continue;
            }
            String link = copy.optString("link");
            if (!link.equals(links.put(id, link))) {
                changedLinks.add(copy);
            }
            bookmarks.put(id, copy);
        }
        findDuplicates();
        if (!changedLinks.isEmpty()) {
            LinkChecker.checkInBackground(changedLinks, false);
        }
    }

    static boolean inCollection(JSONObject bookmark, int collectionId) {
        int bookmarkCollection = bookmark.optInt("collectionId", UNSORTED);
        if (collectionId == ALL) {
            return bookmarkCollection != TRASH;
        }
        return bookmarkCollection == collectionId;
    }

    static String normalizeLink(String link) {
        Uri uri = Uri.parse(link.trim());
        String host = uri.getHost();
        if (host == null) {
            return link.trim();
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        String path = (uri.getEncodedPath() != null) ? uri.getEncodedPath() : "";
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        StringBuilder query = new StringBuilder();
        String encodedQuery = uri.getEncodedQuery();
        if (encodedQuery != null) {
            for (String parameter : encodedQuery.split("&")) {
                if (!parameter.isEmpty() && !parameter.startsWith("utm_")) {
                    query.append((query.length() == 0) ? '?' : '&').append(parameter);
                }
            }
        }
        int port = uri.getPort();
        return host + ((port != -1) ? ":" + port : "") + path + query;
    }

    private static void refreshIfStale() {
        long scannedAccount;
        int indexedCount;
        synchronized (BookmarkIndex.class) {
            long now = SystemClock.elapsedRealtime();
            boolean isFresh = loaded && now - loadedAt < MAX_AGE_MILLIS;
            if (isFresh && (!complete || now - countCheckedAt < COUNT_CHECK_INTERVAL_MILLIS)) {
                return;
            }
            scannedAccount = account;
            indexedCount = isFresh ? bookmarks.size() : -1;
            countCheckedAt = now;
        }
        try {
            if (scannedAccount == 0) {
                scannedAccount = RaindropApi.get("user").getJSONObject("user").getLong("_id");
                useAccount(scannedAccount);
            }
            if (indexedCount >= 0 && serverCount() == indexedCount) {
                return;
            }
            Map<Long, JSONObject> scanned = new LinkedHashMap<>();
            boolean isWholeLibrary = scan(scanned);
            List<JSONObject> published;
            synchronized (BookmarkIndex.class) {
                if (account != scannedAccount) {
                    return;
                }
                bookmarks.clear();
                bookmarks.putAll(scanned);
                links.clear();
                for (Map.Entry<Long, JSONObject> entry : scanned.entrySet()) {
                    links.put(entry.getKey(), entry.getValue().optString("link"));
                }
                loaded = true;
                complete = isWholeLibrary;
                hasScanned = true;
                loadedAt = SystemClock.elapsedRealtime();
                findDuplicates();
                published = new ArrayList<>(bookmarks.values());
                BookmarkIndex.class.notifyAll();
            }
            save(scannedAccount, published);
            LinkChecker.checkInBackground(published, isWholeLibrary);
        } catch (IOException | JSONException ex) {
            Log.w(TAG, "Bookmark scan failed", ex);
        }
    }

    private static void save(long savedAccount, List<JSONObject> published) {
        File target = savedIndex;
        if (target == null) {
            return;
        }
        File temporary = new File(target.getPath() + ".tmp");
        try {
            JSONObject saved = new JSONObject().put("account", savedAccount).put("items", new JSONArray(published));
            try (OutputStream output = new FileOutputStream(temporary)) {
                output.write(saved.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!temporary.renameTo(target)) {
                throw new IOException("Cannot rename " + temporary + " to " + target);
            }
        } catch (IOException | JSONException ex) {
            Log.w(TAG, "Bookmark index not saved", ex);
            temporary.delete();
        }
    }

    private static boolean scan(Map<Long, JSONObject> scanned) throws IOException, JSONException {
        for (int page = 0; page < MAX_PAGES; page++) {
            JSONObject response = RaindropApi.get("raindrops/0?perpage=" + PAGE_SIZE + "&page=" + page);
            if (!response.optBoolean("result")) {
                throw new IOException("raindrops/0 page " + page + " returned result=false");
            }
            JSONArray items = response.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                scanned.put(item.getLong("_id"), item);
            }
            if (items.length() < PAGE_SIZE) {
                return true;
            }
        }
        return false;
    }

    private static int serverCount() throws IOException, JSONException {
        JSONArray stats = RaindropApi.get("user/stats").getJSONArray("items");
        for (int i = 0; i < stats.length(); i++) {
            JSONObject entry = stats.getJSONObject(i);
            if (entry.optInt("_id", Integer.MIN_VALUE) == ALL) {
                return entry.optInt("count");
            }
        }
        throw new IOException("user/stats has no count for collection " + ALL);
    }

    private static void findDuplicates() {
        Map<String, Long> firstByLink = new HashMap<>();
        originals.clear();
        List<JSONObject> oldestFirst = new ArrayList<>(bookmarks.values());
        Collections.sort(oldestFirst, (left, right) -> Long.compare(left.optLong("_id"), right.optLong("_id")));
        for (JSONObject bookmark : oldestFirst) {
            String link = bookmark.optString("link");
            if (link.isEmpty()) {
                continue;
            }
            String key = normalizeLink(link);
            long id = bookmark.optLong("_id");
            Long first = firstByLink.get(key);
            if (first == null) {
                firstByLink.put(key, id);
            } else {
                originals.put(id, first);
            }
        }
    }

}
