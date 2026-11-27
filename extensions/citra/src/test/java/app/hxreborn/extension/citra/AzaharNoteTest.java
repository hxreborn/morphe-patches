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
package app.hxreborn.extension.citra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public final class AzaharNoteTest {

    private static final String PREFERENCES = "hx_citra_patches";

    private static final String SHOWN_KEY = "azahar_note_shown";

    private static boolean shownFlag(Activity activity) {
        return activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(SHOWN_KEY, false);
    }

    private static AlertDialog latestDialog() {
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    @Test
    public void showsDialogOnFirstCall() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        AzaharNote.showOnce(activity);

        Assert.assertNotNull(latestDialog());
        Assert.assertTrue(latestDialog().isShowing());
    }

    @Test
    public void keepsFlagUnsetWhileDialogIsOpen() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        AzaharNote.showOnce(activity);

        Assert.assertFalse(shownFlag(activity));
    }

    @Test
    public void recordsShownFlagWhenDialogIsDismissed() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AzaharNote.showOnce(activity);

        latestDialog().dismiss();
        ShadowLooper.idleMainLooper();

        Assert.assertTrue(shownFlag(activity));
    }

    @Test
    public void showsDialogAgainOnRecreatedActivityBeforeDismissal() {
        AzaharNote.showOnce(Robolectric.buildActivity(Activity.class).setup().get());
        Activity recreated = Robolectric.buildActivity(Activity.class).setup().get();

        AzaharNote.showOnce(recreated);

        Assert.assertTrue(latestDialog().isShowing());
    }

    @Test
    public void skipsDialogWhenAlreadyShown() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(SHOWN_KEY, true).apply();

        AzaharNote.showOnce(activity);

        Assert.assertNull(latestDialog());
    }

}
