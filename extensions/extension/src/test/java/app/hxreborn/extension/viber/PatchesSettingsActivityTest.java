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

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = PatchesSettingsActivityTest.HostIcons.class)
public final class PatchesSettingsActivityTest {

    private static final String ADS_TITLE = "Hide ads";

    private static final String ADS_DESCRIPTION = "Removes ads from the feed.";

    private static final String PLUS_TITLE = "Alternative icons";

    private static final String PLUS_DESCRIPTION = "Uses selected icons.";

    private static final String PICKER_TITLE = "Filter read business chats";

    private static final String RESTART_TITLE = "Restart required";

    private static final long PICKER_TIMEOUT_MS = 5000;

    @After
    public void deleteMessagesDatabase() {
        RuntimeEnvironment.getApplication().deleteDatabase("viber_messages");
    }

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

        Assert.assertEquals(Arrays.asList("Remove chat payment actions", "Disable AI features",
                "Injected disappearing media title", "Injected business name", "Injected folder title",
                "Injected start folder title", "Injected folder limit title", "Hide ads", "Injected promotions title",
                "Hide navigation buttons", "Hide More screen items", "Injected online status title",
                "Disable analytics", "Injected update prompts title", "Injected secondary device title", PLUS_TITLE),
                titles);
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

        Assert.assertEquals(Arrays.asList("Ads", ADS_TITLE, ADS_DESCRIPTION), texts.subList(1, 4));
        Assert.assertFalse(texts.contains("Privacy"));
        Assert.assertFalse(texts.contains("Navigation"));
        int appliedIndex = texts.indexOf("Applied patches");
        Assert.assertEquals(Arrays.asList(ADS_TITLE, ADS_DESCRIPTION),
                texts.subList(appliedIndex + 1, appliedIndex + 3));
    }

    @Test
    @Config(shadows = OnlyPlusApplied.class)
    public void showInjectedPlusMetadataInMenu() {
        List<String> texts = shownTexts();

        Assert.assertEquals(Arrays.asList("App", PLUS_TITLE, PLUS_DESCRIPTION), texts.subList(1, 4));
        int appliedIndex = texts.indexOf("Applied patches");
        Assert.assertEquals(Arrays.asList(PLUS_TITLE, PLUS_DESCRIPTION),
                texts.subList(appliedIndex + 1, appliedIndex + 3));
    }

    @Test
    @Config(shadows = OnlyBusinessChatsApplied.class)
    public void showInjectedBusinessMetadataAndChatPicker() {
        List<String> texts = shownTexts();

        Assert.assertTrue(texts.contains("Chats"));
        Assert.assertTrue(texts.contains("Business chats"));
        Assert.assertFalse(texts.contains("Ads"));
        int appliedIndex = texts.indexOf("Applied patches");
        Assert.assertEquals(Arrays.asList("Injected business name", "Injected business description."),
                texts.subList(appliedIndex + 1, appliedIndex + 3));
        Assert.assertEquals(2, Collections.frequency(texts, "Injected business name"));
    }

    @Test
    @Config(shadows = OnlyBusinessChatsApplied.class)
    public void saveUnchangedBusinessChatsWithoutRestartPrompt() {
        AlertDialog picker = openBusinessChatPicker();

        picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        Assert.assertNotEquals(RESTART_TITLE, latestDialogTitle());
    }

    @Test
    @Config(shadows = OnlyBusinessChatsApplied.class)
    public void saveChangedBusinessChatsAsksForRestart() {
        AlertDialog picker = openBusinessChatPicker();

        picker.getListView().performItemClick(null, 0, 0);
        picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        Assert.assertEquals(RESTART_TITLE, latestDialogTitle());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void showStartFolderDescriptionInSettingsRow() {
        List<String> texts = shownTexts();

        int titleIndex = texts.indexOf("Injected start folder title");
        Assert.assertEquals("Opens Chats in the selected folder.", texts.get(titleIndex + 1));
    }

    @Test
    @Config(shadows = { EveryPatchApplied.class, StartFolderTest.HostFolderAccess.class },
            instrumentedPackages = "app.hxreborn.extension.viber")
    public void askForRestartAfterChangingStartFolder() {
        AlertDialog picker = openStartFolderPicker();

        picker.getListView().performItemClick(null, 1, 1);

        Assert.assertEquals("Unread", PatchSettings.startFolder());
        Assert.assertEquals(RESTART_TITLE, latestDialogTitle());
    }

    @Test
    @Config(shadows = { EveryPatchApplied.class, StartFolderTest.HostFolderAccess.class },
            instrumentedPackages = "app.hxreborn.extension.viber")
    public void dismissUnchangedStartFolderWithoutRestartPrompt() {
        AlertDialog picker = openStartFolderPicker();

        picker.getListView().performItemClick(null, 0, 0);

        Assert.assertFalse(picker.isShowing());
        Assert.assertNotEquals(RESTART_TITLE, latestDialogTitle());
    }

    private static AlertDialog openStartFolderPicker() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.setStartFolder("All");
        StartFolder.initialIndex(Arrays.asList("All", "Unread"), -1, new Object());
        PatchesSettingsActivity activity = Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get();
        clickableParent(findText(activity.getWindow().getDecorView(), "Injected start folder title")).performClick();
        return (AlertDialog) ShadowDialog.getLatestDialog();
    }

    private static AlertDialog openBusinessChatPicker() {
        createBusinessChat();
        PatchesSettingsActivity activity = Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get();
        clickableParent(findText(activity.getWindow().getDecorView(), "Business chats")).performClick();
        long deadline = System.currentTimeMillis() + PICKER_TIMEOUT_MS;
        while (!PICKER_TITLE.equals(latestDialogTitle()) && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper();
            Thread.yield();
        }
        Assert.assertEquals(PICKER_TITLE, latestDialogTitle());
        return (AlertDialog) ShadowDialog.getLatestDialog();
    }

    private static void createBusinessChat() {
        File path = RuntimeEnvironment.getApplication().getDatabasePath("viber_messages");
        path.getParentFile().mkdirs();
        try (SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(path, null)) {
            database
                .execSQL("CREATE TABLE conversations (_id INTEGER PRIMARY KEY, name TEXT, deleted INTEGER DEFAULT 0, "
                        + "conversation_type INTEGER DEFAULT 0, business_inbox_flags INTEGER DEFAULT 1, "
                        + "participant_id_1 INTEGER)");
            database.execSQL("CREATE TABLE participants_info (_id INTEGER PRIMARY KEY, contact_name TEXT, "
                    + "viber_name TEXT)");
            database.execSQL("INSERT INTO conversations (_id, name) VALUES (1, 'Shop')");
        }
    }

    private static String latestDialogTitle() {
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog == null) {
            return null;
        }
        CharSequence title = Shadows.shadowOf(dialog).getTitle();
        return (title != null) ? title.toString() : null;
    }

    private static View findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                View found = findText(group.getChildAt(index), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View clickableParent(View view) {
        View current = view;
        while (!current.hasOnClickListeners()) {
            current = (View) current.getParent();
        }
        return current;
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
    public static final class OnlyBusinessChatsApplied {

        @Implementation
        protected static boolean filterBusinessChats() {
            return true;
        }

        @Implementation
        protected static String filterBusinessChatsName() {
            return "Injected business name";
        }

        @Implementation
        protected static String filterBusinessChatsDescription() {
            return "Injected business description.";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class EveryPatchApplied {

        @Implementation
        protected static boolean startFolder() {
            return true;
        }

        @Implementation
        protected static String startFolderName() {
            return "Injected start folder title";
        }

        @Implementation
        protected static String startFolderDescription() {
            return "Opens Chats in the selected folder.";
        }

        @Implementation
        protected static boolean unfiledChats() {
            return true;
        }

        @Implementation
        protected static String unfiledChatsName() {
            return "Injected folder title";
        }

        @Implementation
        protected static String unfiledChatsDescription() {
            return "Injected unfiledChats description.";
        }

        @Implementation
        protected static boolean disableAiFeatures() {
            return true;
        }

        @Implementation
        protected static String disableAiFeaturesName() {
            return "Disable AI features";
        }

        @Implementation
        protected static String disableAiFeaturesDescription() {
            return "Injected disableAiFeatures description.";
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
        protected static String removeChatPaymentsDescription() {
            return "Removes payment actions from chat menus.";
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
        protected static String hideAdsDescription() {
            return "Injected hideAds description.";
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
        protected static String hideMoreItemsDescription() {
            return "Injected hideMoreItems description.";
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
        protected static String hideNavigationButtonsDescription() {
            return "Injected hideNavigationButtons description.";
        }

        @Implementation
        protected static boolean filterBusinessChats() {
            return true;
        }

        @Implementation
        protected static String filterBusinessChatsName() {
            return "Injected business name";
        }

        @Implementation
        protected static String filterBusinessChatsDescription() {
            return "Injected filterBusinessChats description.";
        }

        @Implementation
        protected static boolean removeOnlineStatusLimit() {
            return true;
        }

        @Implementation
        protected static String removeOnlineStatusLimitName() {
            return "Injected online status title";
        }

        @Implementation
        protected static String removeOnlineStatusLimitDescription() {
            return "Injected removeOnlineStatusLimit description.";
        }

        @Implementation
        protected static boolean hidePromotions() {
            return true;
        }

        @Implementation
        protected static String hidePromotionsName() {
            return "Injected promotions title";
        }

        @Implementation
        protected static String hidePromotionsDescription() {
            return "Injected hidePromotions description.";
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
        protected static String disableAnalyticsDescription() {
            return "Injected disableAnalytics description.";
        }

        @Implementation
        protected static boolean raiseFolderLimit() {
            return true;
        }

        @Implementation
        protected static String raiseFolderLimitName() {
            return "Injected folder limit title";
        }

        @Implementation
        protected static String raiseFolderLimitDescription() {
            return "Allows up to 10 chat folders.";
        }

        @Implementation
        protected static boolean saveDisappearingMedia() {
            return true;
        }

        @Implementation
        protected static String saveDisappearingMediaName() {
            return "Injected disappearing media title";
        }

        @Implementation
        protected static String saveDisappearingMediaDescription() {
            return "Injected saveDisappearingMedia description.";
        }

        @Implementation
        protected static boolean disableUpdatePrompts() {
            return true;
        }

        @Implementation
        protected static String disableUpdatePromptsName() {
            return "Injected update prompts title";
        }

        @Implementation
        protected static String disableUpdatePromptsDescription() {
            return "Injected disableUpdatePrompts description.";
        }

        @Implementation
        protected static boolean linkAsSecondaryDevice() {
            return true;
        }

        @Implementation
        protected static String linkAsSecondaryDeviceName() {
            return "Injected secondary device title";
        }

        @Implementation
        protected static String linkAsSecondaryDeviceDescription() {
            return "Injected secondary device description.";
        }

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
            return "Injected unlockViberPlus description.";
        }

    }

}
