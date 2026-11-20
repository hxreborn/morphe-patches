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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class EqualizerPresetFile {

    static final int MAX_BYTES = 65536;

    private static final String FORMAT = "ultimea-equalizer";

    private static final int LEGACY_FORMAT_VERSION = 1;

    private static final int FORMAT_VERSION = 2;

    private static final Set<String> LEGACY_FIELDS = new HashSet<>(
            Arrays.asList("format", "formatVersion", "name", "frequenciesHz", "gainsTenthsDb"));

    private static final Set<String> FIELDS = new HashSet<>(
            Arrays.asList("format", "formatVersion", "name", "frequenciesHz", "gainsTenthsDb", "mode", "xupmix",
                    "baseName", "baseGainsTenthsDb", "baseMode", "baseXupmix"));

    final String name;

    final EqualizerCurve curve;

    final SoundState state;

    final String baseName;

    final SoundState base;

    final int ignoredFields;

    private EqualizerPresetFile(String name, SoundState state, String baseName, SoundState base, int ignoredFields) {
        this.name = name;
        this.state = state;
        this.curve = state.curve;
        this.baseName = baseName;
        this.base = base;
        this.ignoredFields = ignoredFields;
    }

    static EqualizerPresetFile parse(String document) throws JSONException {
        if (document.length() > MAX_BYTES || document.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new JSONException("Preset file exceeds size limit");
        }
        JSONObject json = new JSONObject(document);
        int version = integer(json.get("formatVersion"), "formatVersion");
        if (!FORMAT.equals(json.get("format")) || (version != LEGACY_FORMAT_VERSION && version != FORMAT_VERSION)) {
            throw new JSONException("Unsupported preset format or version");
        }
        String name = text(json, "name");
        JSONArray frequencies = json.getJSONArray("frequenciesHz");
        if (frequencies.length() != EqualizerCurve.BAND_COUNT) {
            throw new JSONException("Preset must contain ten equalizer bands");
        }
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            if (integer(frequencies.get(band), "frequenciesHz") != EqualizerCurve.frequency(band)) {
                throw new JSONException("Unexpected equalizer frequencies");
            }
        }
        Set<String> fields = (version == LEGACY_FORMAT_VERSION) ? LEGACY_FIELDS : FIELDS;
        int ignored = 0;
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            if (!fields.contains(keys.next())) {
                ignored++;
            }
        }
        try {
            int mode = (version == LEGACY_FORMAT_VERSION) ? EqualizerCurve.CUSTOM_MODE : mode(json, "mode");
            EqualizerCurve curve = curve(json.getJSONArray("gainsTenthsDb"), "gainsTenthsDb", mode);
            if (version == LEGACY_FORMAT_VERSION) {
                SoundState state = new SoundState(curve, -1);
                return new EqualizerPresetFile(EqualizerPreset.validName(name), state, "Custom", state, ignored);
            }
            SoundState state = new SoundState(curve, switchState(json, "xupmix"));
            SoundState base = new SoundState(
                    curve(json.getJSONArray("baseGainsTenthsDb"), "baseGainsTenthsDb", mode(json, "baseMode")),
                    switchState(json, "baseXupmix"));
            return new EqualizerPresetFile(EqualizerPreset.validName(name), state,
                    EqualizerPreset.validName(text(json, "baseName")), base, ignored);
        } catch (IllegalArgumentException ex) {
            throw new JSONException(ex.getMessage());
        }
    }

    EqualizerPresetFile playable() {
        return new EqualizerPresetFile(this.name, EqualizerBases.playable(this.state), this.baseName,
                EqualizerBases.playable(this.base), this.ignoredFields);
    }

    static JSONObject json(String name, EqualizerCurve curve) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("format", FORMAT);
        json.put("formatVersion", LEGACY_FORMAT_VERSION);
        json.put("name", EqualizerPreset.validName(name));
        JSONArray frequencies = new JSONArray();
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            frequencies.put(EqualizerCurve.frequency(band));
        }
        json.put("frequenciesHz", frequencies);
        json.put("gainsTenthsDb", gains(curve));
        return json;
    }

    static JSONObject json(EqualizerPreset preset) throws JSONException {
        return json(preset.name, preset.state, preset.baseName, preset.base);
    }

    JSONObject json() throws JSONException {
        return json(this.name, this.state, this.baseName, this.base);
    }

    private static JSONObject json(String name, SoundState state, String baseName, SoundState base)
            throws JSONException {
        JSONObject json = json(name, state.curve);
        json.put("formatVersion", FORMAT_VERSION);
        json.put("mode", state.curve.mode());
        if (state.xupmix >= 0) {
            json.put("xupmix", state.xupmix == 1);
        }
        json.put("baseName", EqualizerPreset.validName(baseName));
        json.put("baseGainsTenthsDb", gains(base.curve));
        json.put("baseMode", base.curve.mode());
        if (base.xupmix >= 0) {
            json.put("baseXupmix", base.xupmix == 1);
        }
        return json;
    }

    private static JSONArray gains(EqualizerCurve curve) {
        JSONArray gains = new JSONArray();
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            gains.put(curve.gain(band));
        }
        return gains;
    }

    private static EqualizerCurve curve(JSONArray values, String field, int mode) throws JSONException {
        if (values.length() != EqualizerCurve.BAND_COUNT) {
            throw new JSONException("Preset must contain ten equalizer bands");
        }
        int[] gains = new int[EqualizerCurve.BAND_COUNT];
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            gains[band] = integer(values.get(band), field);
        }
        return new EqualizerCurve(mode, gains);
    }

    private static int mode(JSONObject json, String field) throws JSONException {
        int mode = json.has(field) ? integer(json.get(field), field) : EqualizerCurve.CUSTOM_MODE;
        if (mode < 1 || mode > EqualizerPreset.MAX_MODE) {
            throw new JSONException("Unsupported equalizer preset mode");
        }
        return mode;
    }

    private static int switchState(JSONObject json, String field) throws JSONException {
        if (!json.has(field)) {
            return -1;
        }
        Object value = json.get(field);
        if (!(value instanceof Boolean)) {
            throw new JSONException(field + " must be a boolean");
        }
        return ((Boolean) value) ? 1 : 0;
    }

    private static String text(JSONObject json, String field) throws JSONException {
        Object value = json.get(field);
        if (!(value instanceof String)) {
            throw new JSONException(field + " must be text");
        }
        return (String) value;
    }

    private static int integer(Object value, String field) throws JSONException {
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            throw new JSONException(field + " must contain integers");
        }
        long number = ((Number) value).longValue();
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new JSONException(field + " exceeds integer range");
        }
        return (int) number;
    }

}
