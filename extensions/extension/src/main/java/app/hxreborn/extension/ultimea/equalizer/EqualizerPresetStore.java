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

    private static final String RECOVERY_BACKUPS = "recovery_backups";

    private static final String CURRENT = "current";

    private static final String CUSTOM_SUFFIX = " custom";

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
                presets.add(new EqualizerPreset(item.getString("id"), file.name, file.state, file.baseName, file.base));
            }
            return Collections.unmodifiableList(presets);
        }
    }

    int recover() throws JSONException, IOException {
        synchronized (this.preferences) {
            String original = this.preferences.getString(PRESETS, "[]");
            JSONArray entries;
            try {
                entries = new JSONArray(original);
            } catch (JSONException ex) {
                entries = new JSONArray();
            }
            List<EqualizerPreset> readable = new ArrayList<>();
            for (int index = 0; index < entries.length() && readable.size() < MAX_PRESETS; index++) {
                try {
                    JSONObject item = entries.getJSONObject(index);
                    EqualizerPresetFile file = EqualizerPresetFile.parse(item.getJSONObject("curve").toString());
                    String id = item.getString("id");
                    boolean duplicate = false;
                    for (EqualizerPreset preset : readable) {
                        duplicate |= preset.id.equals(id);
                    }
                    if (!id.isEmpty() && !duplicate) {
                        readable.add(new EqualizerPreset(id, file.name, file.state, file.baseName, file.base));
                    }
                } catch (JSONException | IllegalArgumentException ex) {
                    continue;
                }
            }
            JSONArray backups = new JSONArray(this.preferences.getString(RECOVERY_BACKUPS, "[]"));
            if (backups.length() == 0 || !original.equals(backups.getString(backups.length() - 1))) {
                backups.put(original);
            }
            if (!this.preferences.edit().putString(RECOVERY_BACKUPS, backups.toString()).commit()) {
                throw new IOException("Original presets could not be backed up");
            }
            String selected = current();
            boolean retained = false;
            for (EqualizerPreset preset : readable) {
                retained |= preset.id.equals(selected);
            }
            save(readable, retained ? selected : null);
            return readable.size();
        }
    }

    EqualizerPreset add(String name, EqualizerCurve curve) throws JSONException, IOException {
        return add(EqualizerPreset.create(name, curve));
    }

    EqualizerPreset duplicate(EqualizerPreset source, String name) throws JSONException, IOException {
        return add(EqualizerPreset.create(name, source.state, source.baseName, source.base));
    }

    EqualizerPreset add(EqualizerPresetFile file) throws JSONException, IOException {
        return add(EqualizerPreset.create(file.name, file.state, file.baseName, file.base));
    }

    List<EqualizerPreset> addAll(List<EqualizerPresetFile> files) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            if (files.isEmpty()) {
                throw new IOException("No presets to import");
            }
            if (files.size() > MAX_PRESETS - presets.size()) {
                throw new IOException("Local preset limit reached");
            }
            List<EqualizerPreset> added = new ArrayList<>();
            for (EqualizerPresetFile file : files) {
                if (file == null) {
                    throw new IOException("Invalid preset in import");
                }
                EqualizerPreset preset = EqualizerPreset.create(uniqueName(file.name, presets), file.state,
                        file.baseName, file.base);
                presets.add(preset);
                added.add(preset);
            }
            save(presets);
            return Collections.unmodifiableList(added);
        }
    }

    private EqualizerPreset add(EqualizerPreset preset) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            if (presets.size() >= MAX_PRESETS) {
                throw new IOException("Local preset limit reached");
            }
            EqualizerPreset added = new EqualizerPreset(preset.id, uniqueName(preset.name, presets), preset.state,
                    preset.baseName, preset.base);
            presets.add(added);
            save(presets);
            return added;
        }
    }

    EqualizerPreset createCustom(String baseName, SoundState base, SoundState state) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            if (presets.size() >= MAX_PRESETS) {
                throw new IOException("Local preset limit reached");
            }
            String valid = EqualizerPreset.validName(baseName);
            int suffix = 1;
            String candidate;
            do {
                String ending = CUSTOM_SUFFIX + ((suffix == 1) ? "" : " " + suffix);
                candidate = valid.substring(0,
                        Math.min(valid.length(), EqualizerPreset.MAX_NAME_LENGTH - ending.length())) + ending;
                suffix++;
            } while (containsName(presets, candidate));
            EqualizerPreset preset = EqualizerPreset.create(candidate, state, valid, base);
            presets.add(preset);
            save(presets, preset.id);
            return preset;
        }
    }

    void update(String id, SoundState state) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = new ArrayList<>(load());
            for (int index = 0; index < presets.size(); index++) {
                EqualizerPreset preset = presets.get(index);
                if (preset.id.equals(id)) {
                    presets.set(index, new EqualizerPreset(id, preset.name, state, preset.baseName, preset.base));
                    save(presets);
                    return;
                }
            }
            throw new IOException("Preset no longer exists");
        }
    }

    String current() {
        return this.preferences.getString(CURRENT, null);
    }

    void setCurrent(String id) throws JSONException, IOException {
        synchronized (this.preferences) {
            List<EqualizerPreset> presets = load();
            if (id != null) {
                boolean found = false;
                for (EqualizerPreset preset : presets) {
                    if (preset.id.equals(id)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IOException("Preset no longer exists");
                }
            }
            save(presets, id);
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
                    presets.set(index, new EqualizerPreset(id, valid, preset.state, preset.baseName, preset.base));
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
                    save(presets, id.equals(current()) ? null : current());
                    return;
                }
            }
            throw new IOException("Preset no longer exists");
        }
    }

    void prepareExport(EqualizerPreset preset) throws JSONException, IOException {
        synchronized (this.preferences) {
            String document = EqualizerPresetFile.json(preset).toString(2);
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
        save(presets, current());
    }

    private void save(List<EqualizerPreset> presets, String current) throws JSONException, IOException {
        JSONArray json = new JSONArray();
        for (EqualizerPreset preset : presets) {
            JSONObject item = new JSONObject();
            item.put("id", preset.id);
            item.put("curve", EqualizerPresetFile.json(preset));
            json.put(item);
        }
        String previous = this.preferences.getString(PRESETS, null);
        String previousCurrent = current();
        SharedPreferences.Editor edit = this.preferences.edit().putString(PRESETS, json.toString());
        if (current == null) {
            edit.remove(CURRENT);
        } else {
            edit.putString(CURRENT, current);
        }
        if (!edit.commit()) {
            SharedPreferences.Editor rollback = this.preferences.edit();
            if (previous == null) {
                rollback.remove(PRESETS);
            } else {
                rollback.putString(PRESETS, previous);
            }
            if (previousCurrent == null) {
                rollback.remove(CURRENT);
            } else {
                rollback.putString(CURRENT, previousCurrent);
            }
            if (!rollback.commit()) {
                throw new IOException("Local preset save and rollback failed");
            }
            throw new IOException("Local presets could not be saved");
        }
    }

}
