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
package app.hxreborn.extension.viber;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = AiPreferencesTest.EnabledSettings.class,
        instrumentedPackages = "app.hxreborn.extension.viber")
public class AiPreferencesTest {

    @After
    public void resetSetting() {
        EnabledSettings.enabled = true;
    }

    @Test
    public void redirectsPreferenceKeyWhenEnabled() {
        Assert.assertEquals(AiPreferences.DISABLED_KEY_PREFIX + "some_key", AiPreferences.preferenceKey("some_key"));
    }

    @Test
    public void hidesFeatureWhenEnabled() {
        Assert.assertFalse(AiPreferences.showImageRemix(true));
        Assert.assertFalse(AiPreferences.hideFeature(true));
    }

    @Test
    public void marksBannerDismissedWhenEnabled() {
        Assert.assertTrue(AiPreferences.markDismissed(false));
    }

    @Test
    public void preservesHostValuesWhenDisabled() {
        EnabledSettings.enabled = false;
        Assert.assertEquals("some_key", AiPreferences.preferenceKey("some_key"));
        Assert.assertTrue(AiPreferences.showImageRemix(true));
        Assert.assertFalse(AiPreferences.showImageRemix(false));
        Assert.assertTrue(AiPreferences.hideFeature(true));
        Assert.assertFalse(AiPreferences.hideFeature(false));
        Assert.assertFalse(AiPreferences.markDismissed(false));
        Assert.assertTrue(AiPreferences.markDismissed(true));
    }

    @Implements(value = PatchSettings.class, isInAndroidSdk = false)
    public static class EnabledSettings {

        static boolean enabled = true;

        @Implementation
        public static boolean disableAiFeatures() {
            return enabled;
        }

    }

}
