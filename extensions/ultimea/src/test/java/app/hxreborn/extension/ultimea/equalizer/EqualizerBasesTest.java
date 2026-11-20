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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = EqualizerBasesTest.DefinitionsShadow.class)
public class EqualizerBasesTest {

    private static String definitions;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private File cache;

    @Before
    public void prepareDefinitionsAndCache() throws IOException, JSONException {
        try (InputStream input = getClass().getResourceAsStream("/ultimea/equalizer/styles.json")) {
            Assert.assertNotNull(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            JSONObject profile = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            JSONArray gains = new JSONArray();
            for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
                gains.put(0);
            }
            profile.put("default_mode", new JSONArray().put(new JSONObject().put("name", "movie").put("db", gains)));
            definitions = profile.toString();
        }
        this.cache = this.temporary.newFile("ultimea_prod.db");
        ReflectionHelpers.setStaticField(EqualizerBases.class, "cache", this.cache);
        ReflectionHelpers.setStaticField(EqualizerBases.class, "entries", null);
        ReflectionHelpers.setStaticField(EqualizerStyles.class, "points", null);
    }

    @After
    public void clearDefinitionsAndCache() {
        ReflectionHelpers.setStaticField(EqualizerBases.class, "cache", null);
        ReflectionHelpers.setStaticField(EqualizerBases.class, "entries", null);
        ReflectionHelpers.setStaticField(EqualizerStyles.class, "points", null);
        definitions = null;
    }

    @Test
    public void rejectsEmptyBaseModeName() throws JSONException {
        JSONObject profile = new JSONObject(definitions);
        profile.getJSONArray("default_mode").getJSONObject(0).put("name", "");
        definitions = profile.toString();
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsBaseGainBeyondSupportedRange() throws JSONException {
        JSONObject profile = new JSONObject(definitions);
        profile.getJSONArray("default_mode").getJSONObject(0).getJSONArray("db").put(0, 6.1);
        definitions = profile.toString();
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void acceptsAbsentCache() {
        ReflectionHelpers.setStaticField(EqualizerBases.class, "cache", null);
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void acceptsProfileWithoutStyleOverride() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.remove("style");
        write(record(profile));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void acceptsMatchingStyleOnlyProfile() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.remove("default_mode");
        write(record(profile));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void acceptsMatchingCompleteProfile() throws IOException, JSONException {
        write(record(profile()));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedStyleWithUnchangedDefaults() throws IOException, JSONException {
        JSONObject profile = profile();
        point(profile).getJSONArray("db").put(0, 5.4);
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedStyleOnlyProfile() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.remove("default_mode");
        point(profile).getJSONArray("db").put(0, 5.4);
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedPointIdentifier() throws IOException, JSONException {
        JSONObject profile = profile();
        point(profile).put("point", "changed");
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedRowCount() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.put("style", new JSONArray());
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedColumnCount() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.getJSONArray("style").put(0, new JSONArray());
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsChangedBandCount() throws IOException, JSONException {
        JSONObject profile = profile();
        point(profile).put("db", new JSONArray());
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsNonNumericStyleGain() throws IOException, JSONException {
        JSONObject profile = profile();
        point(profile).getJSONArray("db").put(0, "5.5");
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void rejectsMalformedStyleValue() throws IOException, JSONException {
        JSONObject profile = profile();
        profile.put("style", JSONObject.NULL);
        write(record(profile));
        Assert.assertFalse(EqualizerBases.available());
    }

    @Test
    public void acceptsDeletedMismatchedProfile() throws IOException, JSONException {
        JSONObject profile = profile();
        point(profile).put("point", "changed");
        write(record(profile), new JSONObject().put("store", "main").put("key", "model").put("deleted", true));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void acceptsLaterMatchingReplacement() throws IOException, JSONException {
        JSONObject changed = profile();
        point(changed).put("point", "changed");
        write(record(changed), record(profile()));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void acceptsLaterReplacementWithoutEqualizerConfiguration() throws IOException, JSONException {
        JSONObject changed = profile();
        point(changed).put("point", "changed");
        write(record(changed), record(new JSONObject().put("other", true)));
        Assert.assertTrue(EqualizerBases.available());
    }

    @Test
    public void ignoresStyleFromOtherStore() throws IOException, JSONException {
        JSONObject changed = profile();
        point(changed).put("point", "changed");
        write(record(changed).put("store", "other"));
        Assert.assertTrue(EqualizerBases.available());
    }

    private static JSONObject profile() throws JSONException {
        return new JSONObject(definitions);
    }

    private static JSONObject point(JSONObject profile) throws JSONException {
        return profile.getJSONArray("style").getJSONArray(0).getJSONObject(0);
    }

    private static JSONObject record(JSONObject profile) throws JSONException {
        return new JSONObject().put("store", "main").put("key", "model").put("value", profile);
    }

    private void write(JSONObject... records) throws IOException {
        try (FileOutputStream output = new FileOutputStream(this.cache)) {
            for (JSONObject record : records) {
                output.write((record.toString() + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Implements(EqualizerBases.class)
    public static class DefinitionsShadow {

        @Implementation
        protected static String definitions() {
            return EqualizerBasesTest.definitions;
        }

    }

}
