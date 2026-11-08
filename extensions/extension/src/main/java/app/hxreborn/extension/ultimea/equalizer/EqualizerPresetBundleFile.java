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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class EqualizerPresetBundleFile {

    static final int MAX_BYTES = 262144;

    private static final String FORMAT = "ultimea-equalizer-presets";

    private static final int FORMAT_VERSION = 1;

    private static final String SINGLE_FORMAT = "ultimea-equalizer";

    private static final int LEGACY_PRESET_FORMAT_VERSION = 1;

    private static final int FIELD_COUNT = 3;

    private static final int PRESET_FORMAT_VERSION = 2;

    private static final int MAX_PRESETS = 64;

    private EqualizerPresetBundleFile() {
    }

    static JSONObject json(List<EqualizerPreset> presets) throws JSONException {
        requireCount(presets.size());
        JSONArray entries = new JSONArray();
        for (EqualizerPreset preset : presets) {
            entries.put(EqualizerPresetFile.json(preset));
        }
        JSONObject json = new JSONObject();
        json.put("format", FORMAT);
        json.put("formatVersion", FORMAT_VERSION);
        json.put("presets", entries);
        return json;
    }

    static List<EqualizerPresetFile> read(String document) throws JSONException {
        ReadResult result = readReport(document);
        if (!result.rejected.isEmpty()) {
            throw new JSONException("Preset bundle contains invalid sound state files");
        }
        return result.presets;
    }

    static ReadResult readReport(String document) throws JSONException {
        if (document.length() > MAX_BYTES || document.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new JSONException("Preset file exceeds size limit");
        }
        JSONObject json = new JSONObject(document);
        if (!FORMAT.equals(json.get("format"))) {
            if (!SINGLE_FORMAT.equals(json.get("format"))
                    || !(version(json.get("formatVersion"), LEGACY_PRESET_FORMAT_VERSION)
                            || version(json.get("formatVersion"), PRESET_FORMAT_VERSION))) {
                throw new JSONException("Unsupported preset format or version");
            }
            try {
                EqualizerPresetFile file = EqualizerPresetFile.parse(document);
                return new ReadResult(Collections.singletonList(file), Collections.emptyList(), file.ignoredFields);
            } catch (JSONException ex) {
                return new ReadResult(Collections.emptyList(), Collections.singletonList(rejected(1, json)), 0);
            }
        }
        if (!version(json.get("formatVersion"), FORMAT_VERSION)) {
            throw new JSONException("Unsupported preset bundle format or version");
        }
        JSONArray entries = json.getJSONArray("presets");
        requireCount(entries.length());
        List<EqualizerPresetFile> presets = new ArrayList<>();
        List<RejectedPreset> rejected = new ArrayList<>();
        int ignored = json.length() - FIELD_COUNT;
        for (int index = 0; index < entries.length(); index++) {
            JSONObject entry = entries.optJSONObject(index);
            try {
                if (entry == null || !version(entry.get("formatVersion"), PRESET_FORMAT_VERSION)) {
                    throw new JSONException("Preset bundle must contain sound state files");
                }
                EqualizerPresetFile file = EqualizerPresetFile.parse(entry.toString());
                presets.add(file);
                ignored += file.ignoredFields;
            } catch (JSONException ex) {
                rejected.add(rejected(index + 1, entry));
            }
        }
        return new ReadResult(presets, rejected, ignored);
    }

    private static RejectedPreset rejected(int position, JSONObject entry) {
        String name = (entry != null && entry.opt("name") instanceof String) ? (String) entry.opt("name") : null;
        return new RejectedPreset(position, name, RejectionReason.INVALID_SOUND_STATE);
    }

    private static boolean version(Object value, int expected) {
        return (value instanceof Integer || value instanceof Long) && ((Number) value).longValue() == expected;
    }

    private static void requireCount(int count) throws JSONException {
        if (count == 0 || count > MAX_PRESETS) {
            throw new JSONException("Preset bundle must contain between 1 and 64 presets");
        }
    }

    static final class ReadResult {

        final List<EqualizerPresetFile> presets;

        final List<RejectedPreset> rejected;

        final int ignoredFields;

        ReadResult(List<EqualizerPresetFile> presets, List<RejectedPreset> rejected, int ignoredFields) {
            this.presets = Collections.unmodifiableList(new ArrayList<>(presets));
            this.rejected = Collections.unmodifiableList(new ArrayList<>(rejected));
            this.ignoredFields = ignoredFields;
        }

    }

    static final class RejectedPreset {

        final int position;

        final String name;

        final RejectionReason reason;

        RejectedPreset(int position, String name, RejectionReason reason) {
            this.position = position;
            this.name = name;
            this.reason = reason;
        }

    }

    enum RejectionReason {

        INVALID_SOUND_STATE

    }

}
