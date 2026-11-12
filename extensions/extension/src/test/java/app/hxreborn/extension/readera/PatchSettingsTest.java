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

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsTest {

    private SharedPreferences nativePreferences;

    @Before
    public void preparePreferences() {
        this.nativePreferences = PatchSettings.nativePreferences();
        clearPreferences();
    }

    @After
    public void clearPreferences() {
        this.nativePreferences.edit().clear().commit();
    }

    @Test
    public void readNativeDefaults() {
        Assert.assertTrue(PatchSettings.nativeEnabled(PatchSettings.FOOTNOTES, true));
        Assert.assertTrue(PatchSettings.nativeEnabled(PatchSettings.SKIP_HEADERS, true));
        Assert.assertFalse(PatchSettings.nativeEnabled(PatchSettings.FULL_LIBRARY, false));
    }

    @Test
    public void readDisabledNativeMirrors() {
        this.nativePreferences.edit()
            .putBoolean(PatchSettings.FOOTNOTES, false)
            .putBoolean(PatchSettings.SKIP_HEADERS, false)
            .commit();
        Assert.assertFalse(PatchSettings.nativeEnabled(PatchSettings.FOOTNOTES, true));
        Assert.assertFalse(PatchSettings.nativeEnabled(PatchSettings.SKIP_HEADERS, true));
    }

    @Test
    public void restoreNativeSwitchDefaultsAfterReset() {
        PatchSettings.setNativeEnabled(PatchSettings.FOOTNOTES, false);
        PatchSettings.setNativeEnabled(PatchSettings.SKIP_HEADERS, false);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.nativeEnabled(PatchSettings.FOOTNOTES, true));
        Assert.assertTrue(PatchSettings.nativeEnabled(PatchSettings.SKIP_HEADERS, true));
    }

    @Test
    public void readNativeMirrorValues() {
        for (String key : new String[] { PatchSettings.FOOTNOTES, PatchSettings.SKIP_HEADERS,
                PatchSettings.FULL_LIBRARY }) {
            this.nativePreferences.edit().putBoolean(key, true).commit();
            Assert.assertTrue(key, PatchSettings.nativeEnabled(key, false));
        }
    }

    @Test
    public void writeNativeMirrorValues() {
        for (String key : new String[] { PatchSettings.FOOTNOTES, PatchSettings.SKIP_HEADERS,
                PatchSettings.FULL_LIBRARY }) {
            PatchSettings.setNativeEnabled(key, true);
            Assert.assertTrue(key, this.nativePreferences.getBoolean(key, false));
        }
    }

    @Test
    public void writeDisabledNativeMirrors() {
        for (String key : new String[] { PatchSettings.FOOTNOTES, PatchSettings.SKIP_HEADERS,
                PatchSettings.FULL_LIBRARY }) {
            PatchSettings.setNativeEnabled(key, false);
            Assert.assertFalse(key, this.nativePreferences.getBoolean(key, true));
        }
    }

    @Test
    public void keepLibraryFullScreenAfterReset() {
        PatchSettings.setNativeEnabled(PatchSettings.FULL_LIBRARY, true);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.nativeEnabled(PatchSettings.FULL_LIBRARY, false));
    }

}
