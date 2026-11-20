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
package app.hxreborn.extension.readera;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class AccentPaletteTest {

    private static final String PREFERENCE_KEY = "accent_color";

    private static final int ANDROID_11 = 30;

    private static final int ANDROID_12 = 31;

    private Context application;

    private SharedPreferences preferences;

    @Before
    public void preparePreferences() {
        this.application = RuntimeEnvironment.getApplication();
        this.preferences = this.application.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
        clearPreferences();
    }

    @After
    public void clearPreferences() {
        this.preferences.edit().clear().commit();
    }

    @Test
    @Config(sdk = ANDROID_12)
    public void selectMaterialYouByDefaultOnAndroid12() {
        Assert.assertEquals(AccentPalette.SYSTEM, AccentPalette.getPreset(this.application));
    }

    @Test
    @Config(sdk = ANDROID_11)
    public void selectFixedColorByDefaultBelowAndroid12() {
        Assert.assertEquals(AccentPalette.DEFAULT_FIXED.preset, AccentPalette.getPreset(this.application));
        Assert.assertNotEquals(AccentPalette.STOCK, AccentPalette.getPreset(this.application));
    }

    @Test
    @Config(sdk = ANDROID_11)
    public void replaceStoredMaterialYouBelowAndroid12() {
        this.preferences.edit().putString(PREFERENCE_KEY, AccentPalette.SYSTEM).commit();

        Assert.assertEquals(AccentPalette.DEFAULT_FIXED.preset, AccentPalette.getPreset(this.application));
    }

    @Test
    public void saveAndReadEachKindOfChoice() {
        Assert.assertTrue(AccentPalette.setPreset(this.application, AccentPalette.STOCK));
        Assert.assertEquals(AccentPalette.STOCK, AccentPalette.getPreset(this.application));

        Assert.assertTrue(AccentPalette.setPreset(this.application, AccentPalette.Fixed.PINK.preset));
        Assert.assertEquals(AccentPalette.Fixed.PINK.preset, AccentPalette.getPreset(this.application));

        Assert.assertTrue(AccentPalette.setPreset(this.application, "#a738d4"));
        Assert.assertEquals("#A738D4", this.preferences.getString(PREFERENCE_KEY, null));
        Assert.assertEquals("#A738D4", AccentPalette.getPreset(this.application));
    }

    @Test
    public void rejectMalformedCustomColor() {
        Assert.assertFalse(AccentPalette.setPreset(this.application, "#12345"));
        Assert.assertFalse(this.preferences.contains(PREFERENCE_KEY));
    }

    @Test
    public void ignoreUnknownStoredChoice() {
        this.preferences.edit().putString(PREFERENCE_KEY, "yellow").commit();

        Assert.assertEquals(AccentPalette.defaultPreset(), AccentPalette.getPreset(this.application));
    }

    @Test
    public void ignoreStoredChoiceOfWrongType() {
        this.preferences.edit().putInt(PREFERENCE_KEY, 1).commit();

        Assert.assertEquals(AccentPalette.defaultPreset(), AccentPalette.getPreset(this.application));
    }

    @Test
    public void labelEachKindOfChoice() {
        Assert.assertEquals("ReadEra", AccentPalette.label(AccentPalette.STOCK));
        Assert.assertEquals("Material You", AccentPalette.label(AccentPalette.SYSTEM));
        Assert.assertEquals("Blue", AccentPalette.label(AccentPalette.Fixed.BLUE.preset));
        Assert.assertEquals("Custom", AccentPalette.label("#A738D4"));
        Assert.assertTrue(AccentPalette.isCustom("#A738D4"));
        Assert.assertFalse(AccentPalette.isCustom(AccentPalette.Fixed.BLUE.preset));
    }

}
