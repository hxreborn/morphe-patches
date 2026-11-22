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
package app.hxreborn.extension.shared.torrentmenu;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PatchesSettingsActivityBaseTest {

    @Test
    public void renderSectionsAndApplySwitchThenReset() {
        TestPatchesSettingsActivity activity = Robolectric.buildActivity(TestPatchesSettingsActivity.class)
            .setup()
            .get();
        View root = activity.getWindow().getDecorView();
        Assertions.assertThat(findText(root, "Appearance")).isNotNull();
        Assertions.assertThat(findText(root, "First patch")).isNotNull();
        Assertions.assertThat(findText(root, "Second patch")).isNotNull();
        View switchTitle = findText(root, "Test setting");
        ((View) switchTitle.getParent().getParent()).performClick();
        Assertions.assertThat(activity.enabled).isFalse();
        AlertDialog restart = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        Assertions.assertThat(restart.isShowing()).isTrue();
        restart.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        View resetTitle = findText(root, "Reset to defaults");
        ((View) resetTitle.getParent().getParent()).performClick();
        AlertDialog reset = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        reset.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assertions.assertThat(activity.enabled).isTrue();
        Assertions.assertThat(ShadowAlertDialog.getLatestAlertDialog().isShowing()).isTrue();
    }

    @Test
    public void omitOptionalPatchWhenNotApplied() {
        org.robolectric.android.controller.ActivityController<TestPatchesSettingsActivity> controller = Robolectric
            .buildActivity(TestPatchesSettingsActivity.class);
        TestPatchesSettingsActivity activity = controller.get();
        activity.optionalPatch = false;
        controller.setup();
        View root = activity.getWindow().getDecorView();
        Assertions.assertThat(findText(root, "First patch")).isNotNull();
        Assertions.assertThat(findText(root, "Second patch")).isNull();
    }

    private View findText(View view, String text) {
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

}
