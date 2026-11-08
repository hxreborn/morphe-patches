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
package app.hxreborn.extension.proton;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 31, shadows = FloatingNavigationBarTest.Patched.class)
public final class NavigationBarBackdropTest {

    private static final long SURFACE = 0xff2b2b3800000000L;

    private ActivityController<? extends Activity> controller;

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @After
    public void clearState() {
        if (this.controller != null) {
            this.controller.pause().stop().destroy();
        }
        PatchedBuild.useApplicationContext()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    @Config(sdk = { 26, 30 })
    public void retainSolidNavigationBelowAndroid12WithGlassAndBlurSelected() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(7);
        PatchSettings.setEnabled(NavigationBarBackdrop.GLASS_KEY, true);
        this.controller = Robolectric.buildActivity(Activity.class).setup();
        NavigationBarBackdrop.watch(this.controller.get());
        Object original = new Object();
        Assert.assertFalse(NavigationBarBackdrop.isSupported());
        Assert.assertSame(original, NavigationBarBackdrop.source(original));
        Assert.assertSame(original, NavigationBarBackdrop.bar(original));
        Assert.assertEquals(SURFACE, NavigationBarBackdrop.background(SURFACE));
    }

    @Test
    public void disableEffectRowsWithoutDiscardingSelectionsWhenFloatingIsOff() {
        NavigationBarBackdrop.setBlurStrengthDp(12);
        PatchSettings.setEnabled(NavigationBarBackdrop.GLASS_KEY, true);
        Activity activity = settings();
        Switch glass = effectSwitch(activity, "Glass navigation bar");
        Assert.assertFalse(glass.isEnabled());
        Assert.assertTrue(glass.isChecked());
        Assert.assertFalse(((View) glass.getParent()).isEnabled());
        Assert.assertFalse(blurStrengthRow(activity).isEnabled());
        Assert.assertEquals(12, NavigationBarBackdrop.blurStrengthDp());
        Assert.assertTrue(PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false));
    }

    @Test
    @Config(sdk = 30)
    public void explainDisabledEffectRowsOnAndroid11() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        Activity activity = settings();
        Switch glass = effectSwitch(activity, "Glass navigation bar");
        Assert.assertFalse(glass.isEnabled());
        Assert.assertFalse(blurStrengthRow(activity).isEnabled());
        Assert.assertEquals("Glass navigation bar. Requires Android 12 or later.", glass.getContentDescription());
        Assert.assertTrue(containsText(blurStrengthRow(activity), "Requires Android 12 or later."));
    }

    @Test
    public void requireNonzeroBlurWhenGlassIsEnabled() {
        NavigationBarBackdrop.setBlurStrengthDp(0);
        Assert.assertEquals(0, NavigationBarBackdrop.blurStrengthDp());
        PatchSettings.setEnabled(NavigationBarBackdrop.GLASS_KEY, true);
        Assert.assertEquals(7, NavigationBarBackdrop.blurStrengthDp());
        NavigationBarBackdrop.setBlurStrengthDp(0);
        Assert.assertEquals(1, NavigationBarBackdrop.blurStrengthDp());
    }

    @Test
    public void migrateLegacyBlurChoiceToDefaultStrength() {
        PatchSettings.setEnabled("floating_navigation_bar_blur", true);
        Assert.assertEquals(7, NavigationBarBackdrop.blurStrengthDp());
    }

    @Test
    public void useLegacyDefaultWhenSavedStrengthIsMalformed() {
        PatchSettings.setEnabled("floating_navigation_bar_blur", true);
        PatchSettings.setString(NavigationBarBackdrop.BLUR_STRENGTH_KEY, "invalid");
        Assert.assertEquals(7, NavigationBarBackdrop.blurStrengthDp());
    }

    @Test
    public void clampSavedStrengthToSupportedRange() {
        PatchSettings.setString(NavigationBarBackdrop.BLUR_STRENGTH_KEY, "-10");
        Assert.assertEquals(0, NavigationBarBackdrop.blurStrengthDp());
        PatchSettings.setString(NavigationBarBackdrop.BLUR_STRENGTH_KEY, "40");
        Assert.assertEquals(25, NavigationBarBackdrop.blurStrengthDp());
    }

    @Test
    public void retainExplicitOffDespiteLegacyBlurSelection() {
        PatchSettings.setEnabled("floating_navigation_bar_blur", true);
        PatchSettings.setString(NavigationBarBackdrop.BLUR_STRENGTH_KEY, "0");
        Assert.assertEquals(0, NavigationBarBackdrop.blurStrengthDp());
    }

    @Test
    public void saveOffFromSliderWithoutEnablingGlass() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(12);
        Activity activity = settings();
        blurStrengthRow(activity).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        SeekBar slider = findSlider(dialog.findViewById(android.R.id.content));
        Assert.assertNotNull(slider);
        Assert.assertEquals(0, slider.getMin());
        Assert.assertEquals(25, slider.getMax());
        Assert.assertEquals(12, slider.getProgress());
        slider.setProgress(0);
        Assert.assertTrue(containsText(dialog.findViewById(android.R.id.content), "Off"));
        clickText(dialog, "Save");
        Assert.assertEquals(0, NavigationBarBackdrop.blurStrengthDp());
        Assert.assertFalse(PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false));
        dismissRestart();
    }

    @Test
    public void keepGlassSliderAboveZeroAndRequestRestartOnSave() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        PatchSettings.setEnabled(NavigationBarBackdrop.GLASS_KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(7);
        Activity activity = settings();
        blurStrengthRow(activity).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        SeekBar slider = findSlider(dialog.findViewById(android.R.id.content));
        Assert.assertNotNull(slider);
        Assert.assertEquals(1, slider.getMin());
        Assert.assertEquals(25, slider.getMax());
        Assert.assertEquals(7, slider.getProgress());
        AccessibilityNodeInfo.RangeInfo range = slider.createAccessibilityNodeInfo().getRangeInfo();
        Assert.assertNotNull(range);
        Assert.assertEquals(1f, range.getMin(), 0f);
        Assert.assertEquals(25f, range.getMax(), 0f);
        Assert.assertEquals(7f, range.getCurrent(), 0f);
        slider.setProgress(0);
        Assert.assertTrue(containsText(dialog.findViewById(android.R.id.content), "1 / 25"));
        clickText(dialog, "Save");
        Assert.assertEquals(1, NavigationBarBackdrop.blurStrengthDp());
        Assert.assertTrue(PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false));
        dismissRestart();
    }

    @Test
    public void refreshEffectiveStrengthWhenGlassIsEnabled() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(0);
        Activity activity = settings();
        Assert.assertTrue(containsText(blurStrengthRow(activity), "Off"));
        effectSwitch(activity, "Glass navigation bar").performClick();
        Assert.assertTrue(PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false));
        Assert.assertTrue(containsText(blurStrengthRow(activity), "7 / 25"));
        dismissRestart();
    }

    @Test
    public void leaveSavedStrengthUnchangedWhenSliderIsCancelled() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(7);
        Activity activity = settings();
        blurStrengthRow(activity).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        SeekBar slider = findSlider(dialog.findViewById(android.R.id.content));
        Assert.assertNotNull(slider);
        slider.setProgress(20);
        clickText(dialog, "Cancel");
        Assert.assertEquals(7, NavigationBarBackdrop.blurStrengthDp());
        Assert.assertFalse(dialog.isShowing());
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void releaseCoordinatesAndActiveRendererWhenActivityIsDestroyed() throws ReflectiveOperationException {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        NavigationBarBackdrop.setBlurStrengthDp(7);
        this.controller = Robolectric.buildActivity(Activity.class).setup();
        NavigationBarBackdrop.watch(this.controller.get());
        Object renderer = field(NavigationBarBackdrop.class, "current").get(null);
        Assert.assertNotNull(renderer);
        Object source = new Object();
        Object bar = new Object();
        field(renderer.getClass(), "sourceCoordinates").set(renderer, source);
        field(renderer.getClass(), "barCoordinates").set(renderer, bar);
        Assert.assertSame(source, field(renderer.getClass(), "sourceCoordinates").get(renderer));
        Assert.assertSame(bar, field(renderer.getClass(), "barCoordinates").get(renderer));
        this.controller.pause().stop().destroy();
        this.controller = null;
        Assert.assertNull(field(renderer.getClass(), "sourceCoordinates").get(renderer));
        Assert.assertNull(field(renderer.getClass(), "barCoordinates").get(renderer));
        Assert.assertNull(field(NavigationBarBackdrop.class, "current").get(null));
        Assert.assertEquals(SURFACE, NavigationBarBackdrop.background(SURFACE));
    }

    private Activity settings() {
        this.controller = Robolectric.buildActivity(PatchesSettingsActivity.class).setup();
        return this.controller.get();
    }

    private static Switch effectSwitch(Activity activity, String title) {
        Switch control = findSwitch(activity.findViewById(android.R.id.content), title);
        Assert.assertNotNull(title, control);
        return control;
    }

    private static Switch findSwitch(View view, String title) {
        if (view instanceof Switch && view.getContentDescription() != null
                && view.getContentDescription().toString().startsWith(title + ". ")) {
            return (Switch) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                Switch control = findSwitch(group.getChildAt(index), title);
                if (control != null) {
                    return control;
                }
            }
        }
        return null;
    }

    private static View blurStrengthRow(Activity activity) {
        TextView title = findText(activity.findViewById(android.R.id.content), "Blur strength");
        Assert.assertNotNull(title);
        return (View) title.getParent().getParent();
    }

    private static SeekBar findSlider(View view) {
        if (view instanceof SeekBar) {
            return (SeekBar) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                SeekBar slider = findSlider(group.getChildAt(index));
                if (slider != null) {
                    return slider;
                }
            }
        }
        return null;
    }

    private static TextView findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                TextView label = findText(group.getChildAt(index), text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private static void clickText(Dialog dialog, String text) {
        TextView label = findText(dialog.findViewById(android.R.id.content), text);
        Assert.assertNotNull(text, label);
        label.performClick();
    }

    private static void dismissRestart() {
        Dialog dialog = ShadowDialog.getLatestDialog();
        Assert.assertNotNull(dialog);
        Assert.assertTrue(dialog.isShowing());
        Assert.assertTrue(containsText(dialog.findViewById(android.R.id.content), "Restart required"));
        dialog.dismiss();
    }

    private static boolean containsText(View view, String text) {
        return findText(view, text) != null;
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

}
