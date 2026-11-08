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
package app.hxreborn.extension.terabox;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 }, shadows = FakeHostResources.class)
public final class PatchesSettingsDialogTest {

    private Activity activity;

    @Before
    public void startActivity() {
        this.activity = Robolectric.buildActivity(Activity.class).setup().get();
        Utils.setContext(this.activity.getApplication());
    }

    private List<String> shownTexts() {
        showDialog();
        return latestDialogTexts();
    }

    private void showDialog() {
        new PatchesSettingsDialog(this.activity).show();
    }

    private static List<String> latestDialogTexts() {
        List<String> texts = new ArrayList<>();
        for (TextView view : latestDialogTextViews()) {
            texts.add(view.getText().toString());
        }
        return texts;
    }

    private static void clickText(String text) {
        View view = null;
        for (TextView candidate : latestDialogTextViews()) {
            if (text.equals(candidate.getText().toString())) {
                view = candidate;
                break;
            }
        }
        Assert.assertNotNull("No view shows " + text, view);
        while (!view.isClickable() && view.getParent() instanceof View) {
            view = (View) view.getParent();
        }
        Assert.assertTrue("No clickable view holds " + text, view.isClickable());
        view.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static List<TextView> latestDialogTextViews() {
        List<TextView> views = new ArrayList<>();
        collectTextViews(ShadowDialog.getLatestDialog().getWindow().getDecorView(), views);
        return views;
    }

    private static void collectTextViews(View view, List<TextView> views) {
        if (view instanceof TextView) {
            views.add((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectTextViews(group.getChildAt(index), views);
            }
        }
    }

    @After
    public void clearPreferences() {
        this.activity.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void excludesUnappliedSwitchesAndShowsVersions() {
        List<String> texts = shownTexts();
        Assert.assertTrue(texts.contains("About"));
        Assert.assertTrue(texts.contains("App version"));
        Assert.assertTrue(texts.contains("Patch bundle"));
        Assert.assertFalse(texts.contains(AppliedPatches.UNLOCK_PREMIUM));
        Assert.assertFalse(texts.contains("Home"));
        Assert.assertFalse(texts.contains("Videos"));
        Assert.assertFalse(texts.contains("Backup"));
        Assert.assertFalse(texts.contains("Ads"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void groupsSwitchesUnderSectionsNamedAfterWhereTheyShow() {
        List<String> texts = shownTexts();
        Assert.assertFalse(texts.contains("Patches"));
        assertInOrder(texts, "Home", AppliedPatches.HIDE_HOME_SHORTCUTS, AppliedPatches.REMOVE_RECENT_PANEL, "Backup",
                AppliedPatches.SHOW_BACKUP_COUNT, "Videos", AppliedPatches.HIDE_VIDEO_RECOMMENDATIONS,
                AppliedPatches.UNLOCK_PREMIUM, "Ads", AppliedPatches.HIDE_ADS, AppliedPatches.HIDE_PROMOTIONS, "About");
    }

    private static void assertInOrder(List<String> texts, String... expected) {
        int previous = -1;
        for (String text : expected) {
            int index = texts.indexOf(text);
            Assert.assertTrue(text + " is missing", index >= 0);
            Assert.assertTrue(text + " is out of order", index > previous);
            previous = index;
        }
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void showsAllAppliedSwitches() {
        List<String> texts = shownTexts();
        Assert.assertTrue(texts.contains(AppliedPatches.HIDE_ADS));
        Assert.assertTrue(texts.contains(AppliedPatches.HIDE_PROMOTIONS));
        Assert.assertTrue(texts.contains(AppliedPatches.HIDE_HOME_SHORTCUTS));
        Assert.assertTrue(texts.contains(AppliedPatches.HIDE_VIDEO_RECOMMENDATIONS));
        Assert.assertTrue(texts.contains(AppliedPatches.UNLOCK_PREMIUM));
        Assert.assertTrue(texts.contains(AppliedPatches.REMOVE_RECENT_PANEL));
        Assert.assertTrue(texts.contains(AppliedPatches.SHOW_BACKUP_COUNT));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void persistsRestartSwitchAndRequestsRestart() {
        showDialog();
        clickText(AppliedPatches.HIDE_HOME_SHORTCUTS);
        Assert.assertFalse(PatchSettings.isHomeShortcutsHidden());
        Assert.assertTrue(latestDialogTexts().contains("Restart required"));
        Assert.assertTrue(latestDialogTexts().contains("TeraBox must restart to apply the change."));
        Assert.assertTrue(latestDialogTexts().contains("Later"));
        Assert.assertTrue(latestDialogTexts().contains("Restart"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void requestsRestartAfterDisablingBackupCount() {
        showDialog();
        clickText(AppliedPatches.SHOW_BACKUP_COUNT);
        Assert.assertFalse(PatchSettings.isBackupCountShown());
        Assert.assertTrue(latestDialogTexts().contains("Restart required"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void persistsLiveSwitchWithoutRestartDialog() {
        showDialog();
        clickText(AppliedPatches.REMOVE_RECENT_PANEL);
        Assert.assertFalse(PatchSettings.isRecentPanelRemoved());
        Assert.assertFalse(latestDialogTexts().contains("Restart required"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void resetsSwitchesAndRequestsRestart() {
        PatchSettings.setEnabled(AppliedPatches.REMOVE_RECENT_PANEL, false);
        showDialog();
        clickText("Reset to defaults");
        Assert.assertTrue(latestDialogTexts().contains("Reset to defaults?"));
        Assert.assertTrue(latestDialogTexts().contains("Restores every switch to its default."));
        clickText("Reset");
        Assert.assertTrue(PatchSettings.isRecentPanelRemoved());
        Assert.assertTrue(latestDialogTexts().contains("Restart required"));
    }

    @Test
    public void omitsAppliedPatchesSectionWhenNothingIsApplied() {
        Assert.assertFalse(shownTexts().contains("Applied patches"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void listsEveryAppliedPatchAfterAboutWithItsDescriptionCollapsed() {
        showDialog();
        List<String> texts = latestDialogTexts();
        assertInOrder(texts, "About", "Patch bundle", "Applied patches");
        List<String> appliedRows = texts.subList(texts.indexOf("Applied patches") + 1, texts.size());
        List<TextView> textViews = latestDialogTextViews();
        List<AppliedPatches.AppliedPatch> applied = AppliedPatches.applied();
        for (AppliedPatches.AppliedPatch patch : applied) {
            Assert.assertTrue(patch.name + " is missing", appliedRows.contains(patch.name));
            Assert.assertEquals(View.GONE, textViews.get(texts.lastIndexOf(patch.description)).getVisibility());
        }
        Assert.assertEquals(applied.size() * 2, appliedRows.size());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void showsDescriptionWhenAppliedPatchRowIsClicked() {
        showDialog();
        String description = AppliedPatches.cloneAppDescription();

        clickText(AppliedPatches.cloneAppName());

        Assert.assertEquals(View.VISIBLE, latestDialogTextView(description).getVisibility());
        Assert.assertEquals(View.GONE,
                latestDialogTextView(AppliedPatches.hideConsentFormDescription()).getVisibility());
    }

    private static TextView latestDialogTextView(String text) {
        for (TextView candidate : latestDialogTextViews()) {
            if (text.equals(candidate.getText().toString())) {
                return candidate;
            }
        }
        throw new AssertionError("No view shows " + text);
    }

}
