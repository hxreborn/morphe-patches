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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class EqualizerPresetFileTest {

    @Test
    public void roundTripSignedTenths() throws JSONException {
        EqualizerCurve curve = new EqualizerCurve(8, new int[10]).withGain(5, -1);
        EqualizerPresetFile file = EqualizerPresetFile.parse(EqualizerPresetFile.json("Dialogue", curve).toString());
        Assert.assertEquals("Dialogue", file.name);
        Assert.assertEquals(curve, file.curve);
        Assert.assertEquals(0, file.ignoredFields);
    }

    @Test
    public void exportOnlyNameAndBands() throws JSONException {
        JSONObject json = fixture();
        Assert.assertEquals(5, json.length());
        Assert.assertFalse(json.has("id"));
        Assert.assertFalse(json.has("mode"));
        Assert.assertFalse(json.has("device"));
        Assert.assertFalse(json.has("preferences"));
    }

    @Test
    public void ignoreAndCountUnknownFieldsWithoutSelectingMode() throws JSONException {
        JSONObject json = fixture();
        json.put("mode", 9);
        json.put("future", true);
        EqualizerPresetFile file = EqualizerPresetFile.parse(json.toString());
        Assert.assertEquals(2, file.ignoredFields);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, file.curve.mode());
    }

    @Test(expected = JSONException.class)
    public void rejectFractionalTenths() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("gainsTenthsDb").put(5, -0.1);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectNumericStrings() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("gainsTenthsDb").put(5, "-1");
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectReorderedFrequencies() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("frequenciesHz").put(0, 62);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectMissingBand() throws JSONException {
        JSONObject json = fixture();
        json.put("gainsTenthsDb", new JSONArray(new int[9]));
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectGainBeyondNativeRange() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("gainsTenthsDb").put(5, 61);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectNewerFormat() throws JSONException {
        JSONObject json = fixture();
        json.put("formatVersion", 2);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectWrongFormat() throws JSONException {
        JSONObject json = fixture();
        json.put("format", "other");
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectControlCharactersInName() throws JSONException {
        JSONObject json = fixture();
        json.put("name", "Dialogue\ncopy");
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectOversizedDocument() throws JSONException {
        JSONObject json = fixture();
        json.put("unknown", new String(new char[EqualizerPresetFile.MAX_BYTES]));
        EqualizerPresetFile.parse(json.toString());
    }

    private static JSONObject fixture() throws JSONException {
        return EqualizerPresetFile.json("Dialogue", new EqualizerCurve(8, new int[10]));
    }

}
