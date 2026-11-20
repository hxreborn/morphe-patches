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
package app.hxreborn.extension.ultimea;

import android.content.Context;
import android.content.res.Configuration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 31, shadows = EqualizerStyleTest.AppliedFlagsShadow.class)
public class EqualizerStyleTest {

    @After
    public void clearAccent() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void followsCustomAccentInLightAndDark() {
        Assert.assertTrue(AccentColor.isPatched());
        Context context = RuntimeEnvironment.getApplication();
        Assert.assertTrue(AccentPalette.setPreset(context, AccentPalette.Fixed.BLUE.preset));
        for (int night : new int[] { Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES }) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
            Context themed = context.createConfigurationContext(configuration);
            boolean dark = night == Configuration.UI_MODE_NIGHT_YES;
            EqualizerStyle style = new EqualizerStyle(themed);
            Assert.assertEquals(AccentPalette.primary(themed, dark), style.buttonText);
            Assert.assertNotEquals(dark ? AccentPalette.STOCK_DARK_PRIMARY : AccentPalette.STOCK_LIGHT_PRIMARY,
                    style.buttonText);
        }
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedFlagsShadow {

        @Implementation
        protected static boolean accentColor() {
            return true;
        }

    }

}
