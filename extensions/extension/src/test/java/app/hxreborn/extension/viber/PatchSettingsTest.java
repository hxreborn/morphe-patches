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

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class PatchSettingsTest {

    private static final String ADS = "Hide ads";

    private static final long LAST_CHANGE_TIME = 1_793_917_000_000L;

    private static final int SERVER_FOLDER_LIMIT = 5;

    private static final int RAISED_FOLDER_LIMIT = 10;

    private static final int HIGHER_SERVER_FOLDER_LIMIT = 12;

    @Before
    public void initializePreferences() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @After
    public void resetPreferences() {
        PatchSettings.resetToDefaults();
    }

    @Test
    public void retainDisabledPreferenceAfterInitialization() {
        PatchSettings.setEnabled(ADS, false);
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        Assert.assertFalse(PatchSettings.isEnabled(ADS));
    }

    @Test
    public void restoreDefaultAfterDisabling() {
        PatchSettings.setEnabled(ADS, false);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.isEnabled(ADS));
    }

    @Test
    public void keepUnappliedPatchDisabled() {
        PatchSettings.setEnabled(ADS, true);
        Assert.assertFalse(PatchSettings.hideAds());
    }

    @Test
    public void preserveOriginalListWithoutAppliedPatch() {
        List<String> original = Arrays.asList("Settings", "Viber Plus");
        Assert.assertSame(original, PatchSettings.filterMoreItems(original));
    }

    @Test
    public void preserveVisibilityWithoutMenuItem() {
        Assert.assertTrue(PatchSettings.mainMenuItemVisible(null, true));
        Assert.assertFalse(PatchSettings.mainMenuItemVisible(null, false));
    }

    @Test
    public void preserveTypingPrivacyWithoutAppliedPatch() {
        Assert.assertFalse(PatchSettings.unlockTypingPrivacy(false));
        Assert.assertTrue(PatchSettings.unlockTypingPrivacy(true));
    }

    @Test
    @Config(shadows = AppliedPlus.class)
    public void enableTypingPrivacyWithAppliedPatch() {
        Assert.assertTrue(PatchSettings.unlockTypingPrivacy(false));
    }

    @Test
    @Config(shadows = AppliedPlus.class)
    public void preserveTypingPrivacyWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.UNLOCK_VIBER_PLUS.key, false);
        Assert.assertFalse(PatchSettings.unlockTypingPrivacy(false));
        Assert.assertTrue(PatchSettings.unlockTypingPrivacy(true));
    }

    @Test
    public void keepOnlineStatusChangeTimeWithoutAppliedPatch() {
        Assert.assertEquals(LAST_CHANGE_TIME, PatchSettings.lastOnlineChangeTime(LAST_CHANGE_TIME));
    }

    @Test
    @Config(shadows = AppliedOnlineStatusLimit.class)
    public void clearOnlineStatusChangeTimeWithAppliedPatch() {
        Assert.assertEquals(0L, PatchSettings.lastOnlineChangeTime(LAST_CHANGE_TIME));
    }

    @Test
    @Config(shadows = AppliedOnlineStatusLimit.class)
    public void keepOnlineStatusChangeTimeWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.REMOVE_ONLINE_STATUS_LIMIT.key, false);
        Assert.assertEquals(LAST_CHANGE_TIME, PatchSettings.lastOnlineChangeTime(LAST_CHANGE_TIME));
    }

    @Implements(AppliedPatches.class)
    public static class AppliedOnlineStatusLimit {

        @Implementation
        protected static boolean removeOnlineStatusLimit() {
            return true;
        }

    }

    @Test
    public void showPromotionWithoutAppliedPatch() {
        Assert.assertTrue(PatchSettings.showPromotion(true));
    }

    @Test
    @Config(shadows = AppliedPromotions.class)
    public void hidePromotionWithAppliedPatch() {
        Assert.assertFalse(PatchSettings.showPromotion(true));
    }

    @Test
    @Config(shadows = AppliedPromotions.class)
    public void showPromotionWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.HIDE_PROMOTIONS.key, false);
        Assert.assertTrue(PatchSettings.showPromotion(true));
    }

    @Test
    public void keepHiddenPromotionHidden() {
        Assert.assertFalse(PatchSettings.showPromotion(false));
    }

    @Implements(AppliedPatches.class)
    public static class AppliedPromotions {

        @Implementation
        protected static boolean hidePromotions() {
            return true;
        }

    }

    @Test
    public void allowUpdatePromptWithoutAppliedPatch() {
        Assert.assertTrue(PatchSettings.allowUpdatePrompt(true));
    }

    @Test
    @Config(shadows = AppliedUpdatePrompts.class)
    public void blockUpdatePromptWithAppliedPatch() {
        Assert.assertFalse(PatchSettings.allowUpdatePrompt(true));
    }

    @Test
    @Config(shadows = AppliedUpdatePrompts.class)
    public void allowUpdatePromptWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.DISABLE_UPDATE_PROMPTS.key, false);
        Assert.assertTrue(PatchSettings.allowUpdatePrompt(true));
    }

    @Test
    public void keepServerDisabledUpdatePromptOff() {
        Assert.assertFalse(PatchSettings.allowUpdatePrompt(false));
    }

    @Test
    public void keepServerFolderLimitWithoutAppliedPatch() {
        Assert.assertEquals(SERVER_FOLDER_LIMIT, PatchSettings.folderLimit(SERVER_FOLDER_LIMIT));
    }

    @Test
    @Config(shadows = AppliedFolderLimit.class)
    public void raiseFolderLimitWithAppliedPatch() {
        Assert.assertEquals(RAISED_FOLDER_LIMIT, PatchSettings.folderLimit(SERVER_FOLDER_LIMIT));
    }

    @Test
    @Config(shadows = AppliedFolderLimit.class)
    public void keepServerFolderLimitWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.RAISE_FOLDER_LIMIT.key, false);
        Assert.assertEquals(SERVER_FOLDER_LIMIT, PatchSettings.folderLimit(SERVER_FOLDER_LIMIT));
    }

    @Test
    @Config(shadows = AppliedFolderLimit.class)
    public void keepHigherServerFolderLimit() {
        Assert.assertEquals(HIGHER_SERVER_FOLDER_LIMIT, PatchSettings.folderLimit(HIGHER_SERVER_FOLDER_LIMIT));
    }

    @Implements(AppliedPatches.class)
    public static class AppliedFolderLimit {

        @Implementation
        protected static boolean raiseFolderLimit() {
            return true;
        }

    }

    @Implements(AppliedPatches.class)
    public static class AppliedUpdatePrompts {

        @Implementation
        protected static boolean disableUpdatePrompts() {
            return true;
        }

    }

    @Test
    public void keepAnalyticsCollectionWithoutAppliedPatch() {
        Assert.assertEquals(Boolean.TRUE, PatchSettings.analyticsCollectionEnabled(Boolean.TRUE));
        Assert.assertNull(PatchSettings.analyticsCollectionEnabled(null));
    }

    @Test
    @Config(shadows = AppliedAnalytics.class)
    public void disableAnalyticsCollectionWithAppliedPatch() {
        Assert.assertEquals(Boolean.FALSE, PatchSettings.analyticsCollectionEnabled(Boolean.TRUE));
        Assert.assertEquals(Boolean.FALSE, PatchSettings.analyticsCollectionEnabled(null));
    }

    @Test
    @Config(shadows = AppliedAnalytics.class)
    public void keepAnalyticsCollectionWithDisabledPatch() {
        PatchSettings.setEnabled(PatchSetting.DISABLE_ANALYTICS.key, false);
        Assert.assertEquals(Boolean.TRUE, PatchSettings.analyticsCollectionEnabled(Boolean.TRUE));
    }

    @Implements(AppliedPatches.class)
    public static class AppliedAnalytics {

        @Implementation
        protected static boolean disableAnalytics() {
            return true;
        }

    }

    @Implements(AppliedPatches.class)
    public static class AppliedPlus {

        @Implementation
        protected static boolean unlockViberPlus() {
            return true;
        }

        @Implementation
        protected static String unlockViberPlusName() {
            return "Plus controls";
        }

        @Implementation
        protected static String unlockViberPlusDescription() {
            return "Plus controls";
        }

    }

}
