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

import java.util.Arrays;
import java.util.List;

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
public class EqualizerPresetBundleFileTest {

    @Test
    public void roundTripWholeSoundStatesWithoutLocalIdentity() throws JSONException {
        SoundState movie = movie();
        SoundState custom = new SoundState(movie.curve.withGain(5, -1), 0);
        EqualizerPreset first = EqualizerPreset.create("Movie custom", new SoundState(movie.curve, 0), "Movie", movie);
        EqualizerPreset second = EqualizerPreset.create("Dialogue", custom, "Movie", movie);
        JSONObject json = EqualizerPresetBundleFile.json(Arrays.asList(first, second));
        Assert.assertEquals("ultimea-equalizer-presets", json.getString("format"));
        Assert.assertEquals(1, json.getInt("formatVersion"));
        Assert.assertFalse(json.has("current"));
        Assert.assertFalse(json.has("id"));
        JSONArray entries = json.getJSONArray("presets");
        Assert.assertFalse(entries.getJSONObject(0).has("id"));
        Assert.assertFalse(entries.getJSONObject(1).has("id"));
        List<EqualizerPresetFile> files = EqualizerPresetBundleFile.read(json.toString());
        Assert.assertEquals(2, files.size());
        Assert.assertEquals("Movie custom", files.get(0).name);
        Assert.assertEquals(new SoundState(movie.curve, 0), files.get(0).state);
        Assert.assertEquals(movie, files.get(0).base);
        Assert.assertEquals("Dialogue", files.get(1).name);
        Assert.assertEquals(custom, files.get(1).state);
        Assert.assertEquals(movie, files.get(1).base);
        Assert.assertEquals("Movie", files.get(1).baseName);
    }

    @Test
    public void readLegacySinglePresetWithoutInventingXupmix() throws JSONException {
        JSONObject single = EqualizerPresetFile.json("Legacy", movie().curve);
        List<EqualizerPresetFile> files = EqualizerPresetBundleFile.read(single.toString());
        Assert.assertEquals(1, files.size());
        Assert.assertEquals("Legacy", files.get(0).name);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, files.get(0).state.curve.mode());
        Assert.assertEquals(-1, files.get(0).state.xupmix);
    }

    @Test
    public void readWholeStateSinglePreset() throws JSONException {
        EqualizerPreset preset = preset();
        List<EqualizerPresetFile> files = EqualizerPresetBundleFile.read(EqualizerPresetFile.json(preset).toString());
        Assert.assertEquals(1, files.size());
        Assert.assertEquals(preset.state, files.get(0).state);
        Assert.assertEquals(preset.base, files.get(0).base);
    }

    @Test(expected = JSONException.class)
    public void rejectEmptyBundle() throws JSONException {
        JSONObject json = fixture();
        json.put("presets", new JSONArray());
        EqualizerPresetBundleFile.read(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectBundleBeyondPresetLimit() throws JSONException {
        JSONObject json = fixture();
        JSONArray entries = new JSONArray();
        for (int index = 0; index < 65; index++) {
            entries.put(EqualizerPresetFile.json(preset()));
        }
        json.put("presets", entries);
        EqualizerPresetBundleFile.read(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectMalformedSecondEntry() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("presets").put(new JSONObject());
        EqualizerPresetBundleFile.read(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectLegacyEntryInsideWholeStateBundle() throws JSONException {
        JSONObject json = fixture();
        json.getJSONArray("presets").put(EqualizerPresetFile.json("Legacy", movie().curve));
        EqualizerPresetBundleFile.read(json.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectFractionalVersion() throws JSONException {
        JSONObject json = fixture();
        json.put("formatVersion", 1.5);
        EqualizerPresetBundleFile.read(json.toString());
    }

    @Test
    public void countsIgnoredBundleField() throws JSONException {
        JSONObject json = fixture();
        json.put("current", "external selection");
        EqualizerPresetBundleFile.ReadResult result = EqualizerPresetBundleFile.readReport(json.toString());
        Assert.assertEquals(1, result.ignoredFields);
        Assert.assertEquals(1, result.presets.size());
    }

    @Test(expected = JSONException.class)
    public void rejectUtf8BeyondByteLimit() throws JSONException {
        JSONObject json = fixture();
        char[] characters = new char[2048];
        Arrays.fill(characters, '\u00e9');
        JSONObject entry = EqualizerPresetFile.json(preset());
        entry.put("extra", new String(characters));
        JSONArray entries = new JSONArray();
        for (int index = 0; index < 64; index++) {
            entries.put(entry);
        }
        json.put("presets", entries);
        String document = json.toString();
        Assert.assertTrue(document.length() < EqualizerPresetBundleFile.MAX_BYTES);
        EqualizerPresetBundleFile.read(document);
    }

    private static JSONObject fixture() throws JSONException {
        return EqualizerPresetBundleFile.json(Arrays.asList(preset()));
    }

    private static EqualizerPreset preset() {
        SoundState movie = movie();
        return EqualizerPreset.create("Movie custom", movie, "Movie", movie);
    }

    private static SoundState movie() {
        return new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }), 1);
    }

}
