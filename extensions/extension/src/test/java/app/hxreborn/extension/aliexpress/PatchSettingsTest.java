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
package app.hxreborn.extension.aliexpress;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class PatchSettingsTest {

    @Before
    public void initializePreferences() {
        Context application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        PatchSettings.initialize(application);
    }

    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void enableAppliedFeaturesByDefault() {
        for (String key : AppliedPatches.SWITCH_KEYS) {
            Assert.assertTrue(PatchSettings.isFeatureEnabled(true, key));
            Assert.assertFalse(PatchSettings.isFeatureEnabled(false, key));
        }
    }

    @Test
    public void applyChangesWithoutRestart() {
        PatchSettings.setEnabled(AppliedPatches.REMOVE_PROMOTIONAL_POPUPS, false);

        Assert.assertFalse(PatchSettings.isFeatureEnabled(true, AppliedPatches.REMOVE_PROMOTIONAL_POPUPS));
        Assert.assertTrue(PatchSettings.isFeatureEnabled(true, AppliedPatches.SKIP_SPLASH_PROMOTIONS));
    }

    @Test
    public void restoreDefaultsFromResetDialog() {
        PatchSettings.setEnabled(AppliedPatches.REMOVE_PROMOTIONAL_POPUPS, false);
        PatchSettings.setEnabled(AppliedPatches.SKIP_SPLASH_PROMOTIONS, false);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TextView nativeTitle = new TextView(activity);
        new PatchesSettingsDialog(activity, nativeTitle).show();
        View menu = ShadowDialog.getLatestDialog().getWindow().getDecorView();
        TextView resetAction = findText(menu, "Reset to defaults");
        Assert.assertNotNull(resetAction);
        View resetRow = (View) resetAction.getParent().getParent();
        Assert.assertTrue(resetRow.isClickable());
        resetRow.performClick();
        AlertDialog reset = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertEquals("Cancel", reset.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        Assert.assertEquals("Reset", reset.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        reset.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertTrue(PatchSettings.isFeatureEnabled(true, AppliedPatches.REMOVE_PROMOTIONAL_POPUPS));
        Assert.assertTrue(PatchSettings.isFeatureEnabled(true, AppliedPatches.SKIP_SPLASH_PROMOTIONS));
        Assert.assertSame(reset, ShadowAlertDialog.getLatestAlertDialog());
    }

    private static TextView findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            TextView found = findText(group.getChildAt(index), text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
