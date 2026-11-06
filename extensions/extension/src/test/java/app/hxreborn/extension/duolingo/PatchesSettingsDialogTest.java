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
package app.hxreborn.extension.duolingo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = FakeHostResources.class)
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
        assertNotNull("No view shows " + text, view);
        while (!view.isClickable() && view.getParent() instanceof View) {
            view = (View) view.getParent();
        }
        assertTrue("No clickable view holds " + text, view.isClickable());
        view.performClick();
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

    @Test
    public void showsOnlyAboutSectionWhenNoPatchApplied() {
        List<String> texts = shownTexts();

        assertTrue(texts.contains("About"));
        assertFalse(texts.contains("Learning"));
        assertFalse(texts.contains(AppliedPatches.UNLOCK_PREMIUM));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void showsSwitchRowForEveryAppliedPatch() {
        List<String> texts = shownTexts();

        assertTrue(texts.contains(AppliedPatches.UNLOCK_PREMIUM));
        assertTrue(texts.contains(AppliedPatches.UNLIMITED_RAMPUP_TIME));
        assertTrue(texts.contains(AppliedPatches.DISABLE_DYNAMIC_APP_ICON));
        assertTrue(texts.contains(AppliedPatches.ENABLE_DEBUG_MODE));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void describesSuperInUnlockPremiumRowByDefault() {
        List<String> texts = shownTexts();

        assertTrue(texts.contains("Unlocks Super and keeps energy full."));
        assertTrue(texts.contains(PatchSettings.Subscription.SUPER.description));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void describesMaxInUnlockPremiumRowWhenMaxIsChosen() {
        PatchSettings.setSubscription(PatchSettings.Subscription.MAX);

        List<String> texts = shownTexts();

        assertTrue(texts.contains("Unlocks Max and keeps energy full."));
        assertTrue(texts.contains(PatchSettings.Subscription.MAX.description));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void storesChosenSubscriptionAndAsksToRestart() {
        // given
        showDialog();
        clickText("Subscription");

        // when
        clickText(PatchSettings.Subscription.MAX.label);

        // then
        assertEquals(PatchSettings.Subscription.MAX, PatchSettings.subscription());
        assertTrue(latestDialogTexts().contains("Restart required"));
    }

    @Test
    public void showsResetAndCancelLabelsInMixedCase() {
        // given
        showDialog();

        // when
        clickText("Reset to defaults");

        // then
        List<String> texts = latestDialogTexts();
        assertTrue(texts.contains("Reset"));
        assertTrue(texts.contains("Cancel"));
    }

    @Test
    public void showsRestartAndLaterLabelsInMixedCaseAfterReset() {
        // given
        showDialog();
        clickText("Reset to defaults");

        // when
        clickText("Reset");

        // then
        List<String> texts = latestDialogTexts();
        assertTrue(texts.contains("Restart"));
        assertTrue(texts.contains("Later"));
    }

}
