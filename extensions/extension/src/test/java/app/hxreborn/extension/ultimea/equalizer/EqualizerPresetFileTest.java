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
    public void roundTripSoundStateAndBase() throws JSONException {
        SoundState base = movie();
        SoundState state = new SoundState(base.curve, 0);
        EqualizerPreset preset = EqualizerPreset.create("Movie custom", state, "Movie", base);
        JSONObject json = EqualizerPresetFile.json(preset);
        EqualizerPresetFile file = EqualizerPresetFile.parse(json.toString());
        Assert.assertEquals(2, json.getInt("formatVersion"));
        Assert.assertEquals(1, json.getInt("mode"));
        Assert.assertEquals(1, json.getInt("baseMode"));
        Assert.assertEquals("Movie", file.baseName);
        Assert.assertEquals(state.curve, file.state.curve);
        Assert.assertEquals(0, file.state.xupmix);
        Assert.assertEquals(base.curve, file.base.curve);
        Assert.assertEquals(1, file.base.xupmix);
        Assert.assertEquals(0, file.ignoredFields);
        EqualizerPresetFile exported = EqualizerPresetFile.parse(file.json().toString());
        Assert.assertEquals(state.curve, exported.state.curve);
        Assert.assertEquals(base.curve, exported.base.curve);
        Assert.assertEquals(0, exported.state.xupmix);
        Assert.assertEquals(1, exported.base.xupmix);
    }

    @Test
    public void defaultMissingSoundStateModesToCustom() throws JSONException {
        SoundState base = movie();
        JSONObject json = EqualizerPresetFile.json(EqualizerPreset.create("Movie custom", base, "Movie", base));
        json.remove("mode");
        json.remove("baseMode");
        EqualizerPresetFile file = EqualizerPresetFile.parse(json.toString());
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, file.state.curve.mode());
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, file.base.curve.mode());
        Assert.assertEquals(1, file.state.xupmix);
        Assert.assertEquals(1, file.base.xupmix);
    }

    @Test(expected = JSONException.class)
    public void rejectUnverifiedSoundStateMode() throws JSONException {
        SoundState base = movie();
        JSONObject json = EqualizerPresetFile.json(EqualizerPreset.create("Movie custom", base, "Movie", base));
        json.put("mode", 9);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectUnverifiedBaseMode() throws JSONException {
        SoundState base = movie();
        JSONObject json = EqualizerPresetFile.json(EqualizerPreset.create("Movie custom", base, "Movie", base));
        json.put("baseMode", 9);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectUnverifiedModeInFullPreset() {
        EqualizerPreset.create("Unsupported", new SoundState(new EqualizerCurve(9, new int[10]), 0), "Movie", movie());
    }

    @Test
    public void preserveUnknownXupmixInLegacyFiles() throws JSONException {
        JSONObject json = fixture();
        json.put("xupmix", true);
        EqualizerPresetFile file = EqualizerPresetFile.parse(json.toString());
        Assert.assertEquals(-1, file.state.xupmix);
        Assert.assertEquals(-1, file.base.xupmix);
        Assert.assertFalse(file.json().has("xupmix"));
        Assert.assertFalse(file.json().has("baseXupmix"));
        Assert.assertEquals(1, file.ignoredFields);
    }

    @Test(expected = JSONException.class)
    public void rejectNumericXupmixState() throws JSONException {
        EqualizerPreset preset = EqualizerPreset.create("Movie custom",
                new SoundState(new EqualizerCurve(7, new int[10]), 1), "Movie",
                new SoundState(new EqualizerCurve(7, new int[10]), 0));
        JSONObject json = EqualizerPresetFile.json(preset);
        json.put("xupmix", 1);
        EqualizerPresetFile.parse(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectMissingBaseInSoundStateFile() throws JSONException {
        JSONObject json = fixture();
        json.put("formatVersion", 2);
        EqualizerPresetFile.parse(json.toString());
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
        json.put("formatVersion", 3);
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

    private static SoundState movie() {
        return new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }), 1);
    }

}
