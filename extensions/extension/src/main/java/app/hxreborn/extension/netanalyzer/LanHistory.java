/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class LanHistory {

    static final String FAVORITE = "favorite";

    private static final String TAG = "hxNetAnalyzer";

    private static final String PREFERENCES = "hxLanScanHistory";

    private static final String HISTORY_KEY = "history";

    private static final String NAME = "name";

    private static final String IP_START = "ip_start";

    private static final String IP_END = "ip_end";

    private static final String CUSTOM_RANGE = "custom_range";

    private static final String DATE = "date";

    private static final String DEVICES = "devices";

    private static final int KEPT_UNSTARRED = 21;

    private static volatile boolean enabled;

    private static String lastSaved;

    private LanHistory() {
    }

    static void enable() {
        enabled = true;
    }

    static void observe(Context context, boolean running, Object scanInfo) {
        if (!enabled || running) {
            return;
        }
        final LanScan scan = LanScan.read(scanInfo);
        if (scan.devices.isEmpty()) {
            return;
        }
        try {
            final JSONObject item = scan.toJson();
            final String signature = signature(item);
            if (signature.equals(lastSaved)) {
                return;
            }
            lastSaved = signature;
            final List<JSONObject> entries = load(context);
            for (JSONObject entry : entries) {
                if (sameScan(entry, item) && signature(entry).equals(signature)) {
                    return;
                }
            }
            save(context, item);
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot save the LAN scan", ex);
        }
    }

    static List<JSONObject> load(Context context) {
        final List<JSONObject> entries = new ArrayList<>();
        try {
            final JSONArray history = new JSONArray(preferences(context).getString(HISTORY_KEY, "[]"));
            for (int i = 0; i < history.length(); i++) {
                entries.add(history.getJSONObject(i));
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot read the LAN scan history", ex);
        }
        Collections.sort(entries, (left, right) -> Long.compare(right.optLong(DATE), left.optLong(DATE)));
        return entries;
    }

    static void delete(Context context, JSONObject item) {
        final List<JSONObject> entries = load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (sameScan(entries.get(i), item)) {
                entries.remove(i);
            }
        }
        write(context, entries);
    }

    static void clearUnstarred(Context context) {
        final List<JSONObject> entries = load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (!entries.get(i).optBoolean(FAVORITE)) {
                entries.remove(i);
            }
        }
        write(context, entries);
    }

    static void setFavorite(Context context, JSONObject item, boolean favorite) {
        final List<JSONObject> entries = load(context);
        try {
            for (JSONObject entry : entries) {
                if (sameScan(entry, item)) {
                    entry.put(FAVORITE, favorite);
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot star a LAN scan", ex);
        }
        write(context, entries);
    }

    private static void save(Context context, JSONObject item) throws JSONException {
        final List<JSONObject> entries = load(context);
        boolean favorite = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (sameScan(entries.get(i), item)) {
                favorite |= entries.get(i).optBoolean(FAVORITE);
                entries.remove(i);
            }
        }
        item.put(DATE, System.currentTimeMillis()).put(FAVORITE, favorite);
        entries.add(0, item);
        final List<JSONObject> kept = new ArrayList<>();
        int unstarred = 0;
        for (JSONObject entry : entries) {
            if (entry.optBoolean(FAVORITE) || unstarred++ < KEPT_UNSTARRED) {
                kept.add(entry);
            }
        }
        write(context, kept);
    }

    private static void write(Context context, List<JSONObject> entries) {
        final JSONArray history = new JSONArray();
        for (JSONObject entry : entries) {
            history.put(entry);
        }
        preferences(context).edit().putString(HISTORY_KEY, history.toString()).apply();
    }

    private static boolean sameScan(JSONObject left, JSONObject right) {
        return left.optString(NAME).equals(right.optString(NAME))
                && left.optString(IP_START).equals(right.optString(IP_START))
                && left.optString(IP_END).equals(right.optString(IP_END))
                && left.optBoolean(CUSTOM_RANGE) == right.optBoolean(CUSTOM_RANGE);
    }

    private static String signature(JSONObject item) {
        final StringBuilder signature = new StringBuilder(item.optString(NAME)).append('|')
            .append(item.optString(IP_START))
            .append('|')
            .append(item.optString(IP_END));
        return signature.append('|').append(item.optJSONArray(DEVICES)).toString();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

}
