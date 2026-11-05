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
    public void disableAiControlsWhenEnabled() {
        Assert.assertFalse(AiPreferences.showImageRemix(true));
        String[] keys = { "display_chat_summary_feature_key", "display_image_remix_feature_key",
                "display_ai_in_chat_feature_key", "ai_reply_is_display_feature_setting_enabled",
                "smart_recap_is_enabled" };
        for (String key : keys) {
            Assert.assertEquals(Boolean.FALSE, AiPreferences.overrideValue(key, Boolean.TRUE));
        }
        Assert.assertTrue(AiPreferences.overrideBoolean("general_ai_features_banner_dismissed_key", false));
    }

    @Test
    public void preserveHostValuesWhenDisabled() {
        EnabledSettings.enabled = false;
        Assert.assertTrue(AiPreferences.showImageRemix(true));
        Assert.assertFalse(AiPreferences.showImageRemix(false));
        Assert.assertTrue(AiPreferences.overrideBoolean("display_chat_summary_feature_key", true));
        Assert.assertFalse(AiPreferences.overrideBoolean("general_ai_features_banner_dismissed_key", false));
    }

    @Test
    public void preserveUnrelatedAndNonBooleanValues() {
        Assert.assertTrue(AiPreferences.overrideBoolean("notifications_enabled", true));
        Assert.assertFalse(AiPreferences.overrideBoolean("notifications_enabled", false));
        Assert.assertNull(AiPreferences.overrideValue("display_chat_summary_feature_key", null));
        Assert.assertEquals("value", AiPreferences.overrideValue("display_chat_summary_feature_key", "value"));
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
