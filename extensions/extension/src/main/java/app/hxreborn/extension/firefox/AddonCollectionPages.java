/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/io/github/forkmaintainers/iceraven/components/PagedAMOAddonsProvider.kt
 */
package app.hxreborn.extension.firefox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class AddonCollectionPages {

    private static final int TIMEOUT_MILLIS = 20_000;

    private static final String NEXT_PAGE = "next";

    private static final String RESULTS = "results";

    private AddonCollectionPages() {

    }

    public static String appendRemainingPages(String firstPage) throws IOException {
        if (!PatchSettings.fullAddonCollection()) {
            return firstPage;
        }
        try {
            JSONObject collection = new JSONObject(firstPage);
            JSONArray results = collection.getJSONArray(RESULTS);
            Set<String> fetched = new HashSet<>();
            String next = nextPage(collection);
            while (next != null && fetched.add(next)) {
                JSONObject page = new JSONObject(fetch(next));
                JSONArray pageResults = page.getJSONArray(RESULTS);
                for (int i = 0; i < pageResults.length(); i++) {
                    results.put(pageResults.get(i));
                }
                next = nextPage(page);
            }
            collection.put(NEXT_PAGE, JSONObject.NULL);
            return collection.toString();
        } catch (JSONException ex) {
            throw new IOException("Cannot parse add-on collection page", ex);
        }
    }

    private static String nextPage(JSONObject page) throws JSONException {
        return page.isNull(NEXT_PAGE) ? null : page.getString(NEXT_PAGE);
    }

    private static String fetch(String pageUrl) throws IOException {
        URL url = new URL(pageUrl);
        if (!"https".equals(url.getProtocol())) {
            throw new IOException("Refusing non-HTTPS add-on collection page " + pageUrl);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Add-on collection page " + pageUrl + " returned HTTP " + status);
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    body.write(buffer, 0, read);
                }
                return body.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

}
