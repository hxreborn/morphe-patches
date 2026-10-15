/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class SpeedHistory {

    private static final String JSON_CONNECTION_TYPE = "connection_type";

    private static final String PREFERENCES = "SpeedHistory";

    private static final String KEY = "history";

    private static final int HISTORY_LIMIT = 100;

    private SpeedHistory() {
    }

    static List<Entry> load(Context context) {
        final List<Entry> entries = new ArrayList<>();
        final String stored = preferences(context).getString(KEY, "[]");
        try {
            final JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject json = array.getJSONObject(i);
                entries.add(new Entry(json.optString(JSON_CONNECTION_TYPE, ""), json.optString("name", ""),
                        json.optLong("date"), json.optLong("download_speed"), json.optLong("upload_speed")));
            }
        } catch (JSONException ex) {
            entries.clear();
        }
        return entries;
    }

    static void add(Context context, long downloadBytesPerSecond, long uploadBytesPerSecond) {
        final List<Entry> entries = load(context);
        entries.add(new Entry(ConnectedNetwork.transportLabel(context), ConnectedNetwork.ssid(context),
                System.currentTimeMillis(), downloadBytesPerSecond, uploadBytesPerSecond));
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry first, Entry second) {
                return Long.compare(first.date, second.date);
            }
        });
        save(context, entries.subList(Math.max(0, entries.size() - HISTORY_LIMIT), entries.size()));
    }

    static void clear(Context context) {
        preferences(context).edit().remove(KEY).apply();
    }

    private static void save(Context context, List<Entry> entries) {
        final JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                final JSONObject json = new JSONObject();
                json.put(JSON_CONNECTION_TYPE, entry.connectionType);
                json.put("name", entry.name);
                json.put("date", entry.date);
                json.put("download_speed", entry.downloadBytesPerSecond);
                json.put("upload_speed", entry.uploadBytesPerSecond);
                array.put(json);
            }
        } catch (JSONException ex) {
            return;
        }
        preferences(context).edit().putString(KEY, array.toString()).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static final class Entry {

        final String connectionType;

        final String name;

        final long date;

        final long downloadBytesPerSecond;

        final long uploadBytesPerSecond;

        Entry(String connectionType, String name, long date, long downloadBytesPerSecond, long uploadBytesPerSecond) {
            this.connectionType = connectionType;
            this.name = name;
            this.date = date;
            this.downloadBytesPerSecond = downloadBytesPerSecond;
            this.uploadBytesPerSecond = uploadBytesPerSecond;
        }

    }

}
