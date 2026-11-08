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

    private static final int FORMAT_VERSION = 1;

    private static final Set<String> FIELDS = new HashSet<>(
            Arrays.asList("format", "formatVersion", "name", "frequenciesHz", "gainsTenthsDb"));

    final String name;

    final EqualizerCurve curve;

    final int ignoredFields;

    private EqualizerPresetFile(String name, EqualizerCurve curve, int ignoredFields) {
        this.name = name;
        this.curve = curve;
        this.ignoredFields = ignoredFields;
    }

    static EqualizerPresetFile parse(String document) throws JSONException {
        if (document.length() > MAX_BYTES || document.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new JSONException("Preset file exceeds size limit");
        }
        JSONObject json = new JSONObject(document);
        if (!FORMAT.equals(json.get("format")) || integer(json, "formatVersion") != FORMAT_VERSION) {
            throw new JSONException("Unsupported preset format or version");
        }
        Object name = json.get("name");
        if (!(name instanceof String)) {
            throw new JSONException("Preset name must be text");
        }
        JSONArray frequencies = json.getJSONArray("frequenciesHz");
        JSONArray values = json.getJSONArray("gainsTenthsDb");
        if (frequencies.length() != EqualizerCurve.BAND_COUNT || values.length() != EqualizerCurve.BAND_COUNT) {
            throw new JSONException("Preset must contain ten equalizer bands");
        }
        int[] gains = new int[EqualizerCurve.BAND_COUNT];
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            if (integer(frequencies.get(band), "frequenciesHz") != EqualizerCurve.frequency(band)) {
                throw new JSONException("Unexpected equalizer frequencies");
            }
            gains[band] = integer(values.get(band), "gainsTenthsDb");
        }
        int ignored = 0;
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            if (!FIELDS.contains(keys.next())) {
                ignored++;
            }
        }
        try {
            return new EqualizerPresetFile(EqualizerPreset.validName((String) name),
                    new EqualizerCurve(EqualizerCurve.CUSTOM_MODE, gains), ignored);
        } catch (IllegalArgumentException ex) {
            throw new JSONException(ex.getMessage());
        }
    }

    static JSONObject json(String name, EqualizerCurve curve) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("format", FORMAT);
        json.put("formatVersion", FORMAT_VERSION);
        json.put("name", EqualizerPreset.validName(name));
        JSONArray frequencies = new JSONArray();
        JSONArray gains = new JSONArray();
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            frequencies.put(EqualizerCurve.frequency(band));
            gains.put(curve.gain(band));
        }
        json.put("frequenciesHz", frequencies);
        json.put("gainsTenthsDb", gains);
        return json;
    }

    private static int integer(JSONObject json, String field) throws JSONException {
        return integer(json.get(field), field);
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
