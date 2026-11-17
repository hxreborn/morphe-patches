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
package app.hxreborn.extension.utorrent;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PatchSettingsTest {

    @Test
    public void restoreDefaultWithoutChangingOtherSettings() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = context.getSharedPreferences("hx_utorrent_patches", Context.MODE_PRIVATE);
        preferences.edit().clear().putBoolean("unrelated_setting", false).commit();
        try {
            PatchSettings.initialize(context);
            Assert.assertTrue(PatchSettings.isEnabled());
            PatchSettings.setEnabled(false);
            Assert.assertFalse(PatchSettings.isEnabled());
            Assert.assertFalse(preferences.getBoolean("prefer_amoled_dark_mode", true));
            PatchSettings.initialize(context);
            Assert.assertFalse(PatchSettings.isEnabled());
            PatchSettings.resetToDefaults();
            Assert.assertTrue(PatchSettings.isEnabled());
            Assert.assertFalse(preferences.getBoolean("unrelated_setting", true));
        } finally {
            preferences.edit().clear().commit();
        }
    }

}
