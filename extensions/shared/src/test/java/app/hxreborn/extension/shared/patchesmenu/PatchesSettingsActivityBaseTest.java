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
package app.hxreborn.extension.shared.patchesmenu;

import android.app.AlertDialog;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 28, 30 })
public class PatchesSettingsActivityBaseTest {

    private static final int LEFT_INSET = 11;

    private static final int TOP_INSET = 22;

    private static final int RIGHT_INSET = 33;

    private static final int BOTTOM_INSET = 44;

    private static final int CUTOUT_LEFT_INSET = 55;

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

    @Test
    public void redrawScreenAndSkipRestartPromptWhenResetFailsToSave() {
        TestPatchesSettingsActivity activity = Robolectric.buildActivity(TestPatchesSettingsActivity.class)
            .setup()
            .get();
        activity.enabled = false;
        activity.resetSaves = false;
        View screen = installedScreen(activity);

        AlertDialog reset = confirmReset(activity);

        Assertions.assertThat(activity.enabled).isTrue();
        Assertions.assertThat(installedScreen(activity)).isNotSameAs(screen);
        Assertions.assertThat(ShadowAlertDialog.getLatestAlertDialog()).isSameAs(reset);
        Assertions.assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Couldn't reset. Try again.");
    }

    @Test
    @Config(sdk = 30)
    public void padInstalledScreenBySystemBarsAndCutoutOnApi30() {
        TestPatchesSettingsActivity activity = Robolectric.buildActivity(TestPatchesSettingsActivity.class)
            .setup()
            .get();
        WindowInsets insets = new WindowInsets.Builder()
            .setInsets(WindowInsets.Type.systemBars(), Insets.of(LEFT_INSET, TOP_INSET, RIGHT_INSET, BOTTOM_INSET))
            .setInsets(WindowInsets.Type.displayCutout(), Insets.of(CUTOUT_LEFT_INSET, 0, 0, 0))
            .build();

        assertConsumedApi30(installedScreen(activity), insets);
        confirmReset(activity);
        assertConsumedApi30(installedScreen(activity), insets);
    }

    @Test
    @Config(sdk = { 23, 28 })
    public void padInstalledScreenBySystemWindowInsetsBeforeApi30() {
        TestPatchesSettingsActivity activity = Robolectric.buildActivity(TestPatchesSettingsActivity.class)
            .setup()
            .get();

        assertConsumedBeforeApi30(installedScreen(activity));
        confirmReset(activity);
        assertConsumedBeforeApi30(installedScreen(activity));
    }

    private void assertConsumedApi30(View screen, WindowInsets insets) {
        WindowInsets remaining = screen.dispatchApplyWindowInsets(insets);

        Assertions.assertThat(screen.getPaddingLeft()).isEqualTo(CUTOUT_LEFT_INSET);
        Assertions.assertThat(screen.getPaddingTop()).isEqualTo(TOP_INSET);
        Assertions.assertThat(screen.getPaddingRight()).isEqualTo(RIGHT_INSET);
        Assertions.assertThat(screen.getPaddingBottom()).isEqualTo(BOTTOM_INSET);
        Assertions.assertThat(remaining.isConsumed()).isTrue();
    }

    private void assertConsumedBeforeApi30(View screen) {
        WindowInsets insets = ReflectionHelpers.callConstructor(WindowInsets.class, ReflectionHelpers.ClassParameter
            .from(Rect.class, new Rect(LEFT_INSET, TOP_INSET, RIGHT_INSET, BOTTOM_INSET)));

        WindowInsets remaining = screen.dispatchApplyWindowInsets(insets);

        assertPadding(screen);
        Assertions.assertThat(remaining.getSystemWindowInsetTop()).isZero();
        Assertions.assertThat(remaining.getSystemWindowInsetBottom()).isZero();
    }

    private View installedScreen(TestPatchesSettingsActivity activity) {
        return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    }

    private AlertDialog confirmReset(TestPatchesSettingsActivity activity) {
        View resetTitle = findText(installedScreen(activity), "Reset to defaults");
        ((View) resetTitle.getParent().getParent()).performClick();
        AlertDialog reset = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        reset.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        return reset;
    }

    private void assertPadding(View screen) {
        Assertions.assertThat(screen.getPaddingLeft()).isEqualTo(LEFT_INSET);
        Assertions.assertThat(screen.getPaddingTop()).isEqualTo(TOP_INSET);
        Assertions.assertThat(screen.getPaddingRight()).isEqualTo(RIGHT_INSET);
        Assertions.assertThat(screen.getPaddingBottom()).isEqualTo(BOTTOM_INSET);
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
