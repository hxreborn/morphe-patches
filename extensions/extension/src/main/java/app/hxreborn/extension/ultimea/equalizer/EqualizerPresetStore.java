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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class EqualizerPresetStore {

    private static final String PREFERENCES = "hx_ultimea_equalizer_presets";

    private static final String PRESETS = "presets";

    private static final String PENDING_EXPORT = "pending_export";

    private static final int MAX_PRESETS = 64;

    private final SharedPreferences preferences;

    EqualizerPresetStore(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    List<EqualizerPreset> load() throws JSONException {
        synchronized (this.preferences) {
            JSONArray json = new JSONArray(this.preferences.getString(PRESETS, "[]"));
            if (json.length() > MAX_PRESETS) {
                throw new JSONException("Too many local presets");
            }
            List<EqualizerPreset> presets = new ArrayList<>();
            for (int index = 0; index < json.length(); index++) {
                JSONObject item = json.getJSONObject(index);
                EqualizerPresetFile file = EqualizerPresetFile.parse(item.getJSONObject("curve").toString());
                presets.add(new EqualizerPreset(item.getString("id"), file.name, file.curve));
            }
            return Collections.unmodifiableList(presets);
        }
    }

    EqualizerPreset add(String name, EqualizerCurve curve) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            if (presets.size() >= MAX_PRESETS) {
                throw new IOException("Local preset limit reached");
            }
            EqualizerPreset preset = EqualizerPreset.create(uniqueName(name, presets), curve);
            presets.add(preset);
            save(presets);
            return preset;
        }
    }

    void rename(String id, String name) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            String valid = EqualizerPreset.validName(name);
            for (EqualizerPreset preset : presets) {
                if (!preset.id.equals(id) && preset.name.equals(valid)) {
                    throw new IllegalArgumentException("A preset already uses this name");
                }
            }
            for (int index = 0; index < presets.size(); index++) {
                EqualizerPreset preset = presets.get(index);
                if (preset.id.equals(id)) {
                    presets.set(index, new EqualizerPreset(id, valid, preset.curve));
                    save(presets);
                    return;
                }
            }
            throw new IOException("Preset no longer exists");
        }
    }

    void remove(String id) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            for (int index = 0; index < presets.size(); index++) {
                if (presets.get(index).id.equals(id)) {
                    presets.remove(index);
                    save(presets);
                    return;
                }
            }
            throw new IOException("Preset no longer exists");
        }
    }

    void prepareExport(EqualizerPreset preset) throws JSONException, IOException {
        synchronized (this.preferences) {
            String document = EqualizerPresetFile.json(preset.name, preset.curve).toString(2);
            if (!this.preferences.edit().putString(PENDING_EXPORT, document).commit()) {
                throw new IOException("Pending export could not be saved");
            }
        }
    }

    EqualizerPresetFile takeExport() throws JSONException, IOException {
        synchronized (this.preferences) {
            String document = this.preferences.getString(PENDING_EXPORT, null);
            if (!this.preferences.edit().remove(PENDING_EXPORT).commit()) {
                throw new IOException("Pending export could not be cleared");
            }
            return (document != null) ? EqualizerPresetFile.parse(document) : null;
        }
    }

    private static String uniqueName(String name, List<EqualizerPreset> presets) {
        String base = EqualizerPreset.validName(name);
        String candidate = base;
        int suffix = 2;
        while (containsName(presets, candidate)) {
            String ending = " (" + suffix++ + ")";
            candidate = base.substring(0, Math.min(base.length(), EqualizerPreset.MAX_NAME_LENGTH - ending.length()))
                    + ending;
        }
        return candidate;
    }

    private static boolean containsName(List<EqualizerPreset> presets, String name) {
        for (EqualizerPreset preset : presets) {
            if (preset.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void save(List<EqualizerPreset> presets) throws JSONException, IOException {
        JSONArray json = new JSONArray();
        for (EqualizerPreset preset : presets) {
            JSONObject item = new JSONObject();
            item.put("id", preset.id);
            item.put("curve", EqualizerPresetFile.json(preset.name, preset.curve));
            json.put(item);
        }
        String previous = this.preferences.getString(PRESETS, null);
        if (!this.preferences.edit().putString(PRESETS, json.toString()).commit()) {
            SharedPreferences.Editor rollback = this.preferences.edit();
            if (previous == null) {
                rollback.remove(PRESETS);
            } else {
                rollback.putString(PRESETS, previous);
            }
            if (!rollback.commit()) {
                throw new IOException("Local preset save and rollback failed");
            }
            throw new IOException("Local presets could not be saved");
        }
    }

}
