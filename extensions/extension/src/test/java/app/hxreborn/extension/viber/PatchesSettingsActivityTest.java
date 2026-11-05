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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = PatchesSettingsActivityTest.HostIcons.class)
public final class PatchesSettingsActivityTest {

    private static final String ADS_TITLE = "Hide ads";

    private static final String ADS_DESCRIPTION = "Removes ads from the feed.";

    private static final String PLUS_TITLE = "Alternative icons";

    private static final String PLUS_DESCRIPTION = "Uses selected icons.";

    @Test
    public void answerNotAppliedForEverySettingBeforePatchesAreApplied() {
        for (PatchSetting setting : PatchSetting.values()) {
            Assert.assertFalse(setting.name(), setting.applied());
        }
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void answerAppliedAndInjectedTitleForEverySetting() {
        List<String> titles = new ArrayList<>();
        for (PatchSetting setting : PatchSetting.values()) {
            Assert.assertTrue(setting.name(), setting.applied());
            titles.add(setting.title());
        }

        Assert.assertEquals(Arrays.asList("Remove chat payment actions", "Disable AI features", "Hide ads",
                "Hide More screen items", "Hide navigation buttons", "Disable analytics", PLUS_TITLE), titles);
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void showOneHeaderForSwitchesSharingSection() {
        List<String> texts = shownTexts();

        Assert.assertEquals(1, Collections.frequency(texts, "Chats"));
    }

    @Test
    public void showNoSwitchSectionBeforePatchesAreApplied() {
        List<String> texts = shownTexts();

        Assert.assertFalse(texts.contains("Ads"));
        Assert.assertFalse(texts.contains("Privacy"));
        Assert.assertFalse(texts.contains("Applied patches"));
    }

    @Test
    @Config(shadows = OnlyAdsApplied.class)
    public void showSwitchAndAppliedRowOnlyForAppliedPatch() {
        List<String> texts = shownTexts();

        Assert.assertEquals(Arrays.asList("Ads", ADS_TITLE, "Removes ads."), texts.subList(1, 4));
        Assert.assertFalse(texts.contains("Privacy"));
        Assert.assertFalse(texts.contains("Navigation bar"));
        int appliedIndex = texts.indexOf("Applied patches");
        Assert.assertEquals(Arrays.asList(ADS_TITLE, ADS_DESCRIPTION),
                texts.subList(appliedIndex + 1, appliedIndex + 3));
    }

    @Test
    @Config(shadows = OnlyPlusApplied.class)
    public void showInjectedPlusMetadataInMenu() {
        List<String> texts = shownTexts();

        Assert.assertEquals(Arrays.asList("Viber Plus", PLUS_TITLE, "Unlocks icons and typing privacy."), texts.subList(1, 4));
        int appliedIndex = texts.indexOf("Applied patches");
        Assert.assertEquals(Arrays.asList(PLUS_TITLE, PLUS_DESCRIPTION),
                texts.subList(appliedIndex + 1, appliedIndex + 3));
    }

    private static List<String> shownTexts() {
        View root = Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get().getWindow().getDecorView();
        List<String> texts = new ArrayList<>();
        collectTexts(root, texts);
        return texts;
    }

    private static void collectTexts(View view, List<String> texts) {
        if (view instanceof TextView && ((TextView) view).getText().length() > 0) {
            texts.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectTexts(group.getChildAt(index), texts);
            }
        }
    }

    @Implements(value = SettingsStyle.class, isInAndroidSdk = false)
    public static final class HostIcons {

        @Implementation
        protected Drawable hostIcon(String name, int tint) {
            return new ColorDrawable(tint);
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class OnlyAdsApplied {

        @Implementation
        protected static boolean hideAds() {
            return true;
        }

        @Implementation
        protected static String hideAdsName() {
            return ADS_TITLE;
        }

        @Implementation
        protected static String hideAdsDescription() {
            return ADS_DESCRIPTION;
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class OnlyPlusApplied {

        @Implementation
        protected static boolean unlockViberPlus() {
            return true;
        }

        @Implementation
        protected static String unlockViberPlusName() {
            return PLUS_TITLE;
        }

        @Implementation
        protected static String unlockViberPlusDescription() {
            return PLUS_DESCRIPTION;
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class EveryPatchApplied {

        @Implementation
        protected static boolean disableAiFeatures() {
            return true;
        }

        @Implementation
        protected static String disableAiFeaturesName() {
            return "Disable AI features";
        }

        @Implementation
        protected static boolean removeChatPayments() {
            return true;
        }

        @Implementation
        protected static String removeChatPaymentsName() {
            return "Remove chat payment actions";
        }

        @Implementation
        protected static boolean hideAds() {
            return true;
        }

        @Implementation
        protected static String hideAdsName() {
            return "Hide ads";
        }

        @Implementation
        protected static boolean hideMoreItems() {
            return true;
        }

        @Implementation
        protected static String hideMoreItemsName() {
            return "Hide More screen items";
        }

        @Implementation
        protected static boolean hideNavigationButtons() {
            return true;
        }

        @Implementation
        protected static String hideNavigationButtonsName() {
            return "Hide navigation buttons";
        }

        @Implementation
        protected static boolean disableAnalytics() {
            return true;
        }

        @Implementation
        protected static String disableAnalyticsName() {
            return "Disable analytics";
        }

        @Implementation
        protected static boolean unlockViberPlus() {
            return true;
        }

        @Implementation
        protected static String unlockViberPlusName() {
            return PLUS_TITLE;
        }

    }

}
