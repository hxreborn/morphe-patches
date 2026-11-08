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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class EqualizerBases {

    private static final long MAX_CACHE_BYTES = 2097152;

    private static File cache;

    private static List<Entry> entries;

    private EqualizerBases() {
    }

    static void configure(Context context) {
        cache = new File(context.getFilesDir(), "Cache/ultimea_prod.db");
    }

    static boolean available() {
        try {
            List<Entry> expected = entries();
            if (cache == null || !cache.isFile()) {
                return true;
            }
            if (cache.length() > MAX_CACHE_BYTES) {
                return false;
            }
            Map<String, JSONObject> profiles = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(cache), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JSONObject record = new JSONObject(line);
                    if (!"main".equals(record.optString("store"))) {
                        continue;
                    }
                    String key = record.optString("key");
                    if (record.optBoolean("deleted")) {
                        profiles.remove(key);
                    } else {
                        JSONObject profile = record.optJSONObject("value");
                        if (profile != null && (profile.has("default_mode") || profile.has("style"))) {
                            profiles.put(key, profile);
                        } else {
                            profiles.remove(key);
                        }
                    }
                }
            }
            for (JSONObject profile : profiles.values()) {
                if (profile.has("style") && !matchesStyles(profile.getJSONArray("style"))) {
                    return false;
                }
                if (!profile.has("default_mode")) {
                    continue;
                }
                JSONArray modes = profile.getJSONArray("default_mode");
                if (modes.length() != expected.size()) {
                    return false;
                }
                for (int mode = 0; mode < modes.length(); mode++) {
                    JSONObject item = modes.getJSONObject(mode);
                    JSONArray gains = item.getJSONArray("db");
                    Entry entry = expected.get(mode);
                    if (!entry.name.equalsIgnoreCase(item.getString("name"))
                            || gains.length() != EqualizerCurve.BAND_COUNT) {
                        return false;
                    }
                    for (int band = 0; band < gains.length(); band++) {
                        if (gains.getDouble(band) * 10 != entry.curve.gain(band)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (IOException | JSONException | IllegalStateException ex) {
            return false;
        }
    }

    private static boolean matchesStyles(JSONArray rows) throws JSONException {
        if (rows.length() != EqualizerStyles.GRID_SIZE) {
            return false;
        }
        for (int row = 0; row < rows.length(); row++) {
            JSONArray columns = rows.getJSONArray(row);
            if (columns.length() != EqualizerStyles.GRID_SIZE) {
                return false;
            }
            for (int column = 0; column < columns.length(); column++) {
                JSONObject item = columns.getJSONObject(column);
                EqualizerStyles.Point expected = EqualizerStyles.at(row, column);
                JSONArray gains = item.getJSONArray("db");
                if (!expected.id.equals(item.get("point")) || gains.length() != EqualizerCurve.BAND_COUNT) {
                    return false;
                }
                for (int band = 0; band < gains.length(); band++) {
                    Object gain = gains.get(band);
                    if (!(gain instanceof Number) || ((Number) gain).doubleValue() * 10 != expected.curve.gain(band)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static String definitions() {
        return "";
    }

    static List<Entry> entries() {
        if (entries != null) {
            return entries;
        }
        try {
            JSONArray modes = new JSONObject(definitions()).getJSONArray("default_mode");
            List<Entry> parsed = new ArrayList<>();
            for (int index = 0; index < modes.length(); index++) {
                JSONObject mode = modes.getJSONObject(index);
                String name = mode.getString("name");
                if (name.isEmpty()) {
                    throw new JSONException("Empty base mode name");
                }
                JSONArray values = mode.getJSONArray("db");
                if (values.length() != EqualizerCurve.BAND_COUNT) {
                    throw new JSONException("Expected ten base bands");
                }
                int[] gains = new int[EqualizerCurve.BAND_COUNT];
                for (int band = 0; band < gains.length; band++) {
                    double tenths = values.getDouble(band) * 10;
                    gains[band] = (int) Math.round(tenths);
                    if (gains[band] != tenths) {
                        throw new JSONException("Base gain exceeds supported precision");
                    }
                }
                parsed.add(new Entry(Character.toUpperCase(name.charAt(0)) + name.substring(1),
                        new EqualizerCurve(index + 1, gains)));
            }
            if (parsed.isEmpty()) {
                throw new JSONException("Missing base modes");
            }
            entries = Collections.unmodifiableList(parsed);
            return entries;
        } catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalStateException("Equalizer bases could not be read", ex);
        }
    }

    static SoundState playable(SoundState state) {
        if (state.curve.mode() < EqualizerCurve.CUSTOM_MODE) {
            Entry entry = mode(state.curve.mode());
            if (entry == null || !entry.curve.equals(state.curve)) {
                return state.withCurve(state.curve.asCustom());
            }
        }
        return state;
    }

    static Entry mode(int mode) {
        if (mode >= EqualizerCurve.CUSTOM_MODE || !available()) {
            return null;
        }
        for (Entry entry : entries()) {
            if (entry.curve.mode() == mode) {
                return entry;
            }
        }
        return null;
    }

    static final class Entry {

        final String name;

        final EqualizerCurve curve;

        Entry(String name, EqualizerCurve curve) {
            this.name = name;
            this.curve = curve;
        }

    }

}
