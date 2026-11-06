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
package app.hxreborn.extension.liftoff;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = EveryPatchApplied.class, sdk = { 23, 36 })
public final class PatchesMenuTest {

    private static final int WINDOW_WIDTH_PX = 1080;

    private static final int WINDOW_HEIGHT_PX = 1920;

    private static final int CONTENT_HEIGHT_PX = WINDOW_HEIGHT_PX * 2;

    private static final int ROW_HEIGHT_PX = 128;

    private static final int HOST_TEXT_COLOR = 0xFFFAFAFA;

    private static final int HOST_BACKGROUND_COLOR = 0xFF0F0A18;

    private static final int HOST_CARD_COLOR = 0xFF1F1A27;

    private static final int LIGHT_HOST_TEXT_COLOR = 0xFF143A49;

    private static final int LIGHT_HOST_BACKGROUND_COLOR = 0xFFEDF7F8;

    private static final int LIGHT_HOST_CARD_COLOR = 0xFFDDE9EB;

    private ActivityController<Activity> controller;

    private Activity activity;

    private ViewGroup root;

    private ScrollView scroll;

    private LinearLayout content;

    @Before
    public void createSettingsScreen() {
        PatchContext.attach(RuntimeEnvironment.getApplication());
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        this.controller = Robolectric.buildActivity(Activity.class).setup().visible();
        this.activity = this.controller.get();
        this.scroll = new ScrollView(this.activity);
        this.content = new LinearLayout(this.activity);
        this.content.setOrientation(LinearLayout.VERTICAL);
        this.content.setMinimumHeight(CONTENT_HEIGHT_PX);
        addLabel("Join Our Discord");
        addLabel("Restore Purchases");
        this.scroll.addView(this.content);
        this.activity.setContentView(this.scroll);
        this.root = this.activity.findViewById(android.R.id.content);
        this.root.measure(View.MeasureSpec.makeMeasureSpec(WINDOW_WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(WINDOW_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        this.root.layout(0, 0, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);
    }

    @After
    public void closeActivity() {
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            dialog.dismiss();
        }
        this.controller.pause().stop().destroy();
        ShadowLooper.idleMainLooper();
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void keepsNativeScrollChildrenWhenAddingEntry() {
        attachEntry();

        Assert.assertSame(this.content, this.scroll.getChildAt(0));
        Assert.assertEquals(1, this.scroll.getChildCount());
        Assert.assertNotNull(this.root.findViewWithTag(this.scroll));
        Assert.assertTrue(this.scroll.getPaddingBottom() > 0);
    }

    @Test
    public void addsOnlyOneEntryWhenLayoutRepeats() {
        attachEntry();
        int children = this.content.getChildCount();

        attachEntry();

        Assert.assertEquals(children, this.content.getChildCount());
    }

    @Test
    public void removesEntryWhenNativeScreenDetaches() {
        attachEntry();

        this.root.removeView(this.scroll);
        ShadowLooper.idleMainLooper();

        Assert.assertNull(this.root.findViewWithTag(this.scroll));
        Assert.assertEquals(0, this.scroll.getPaddingBottom());
    }

    @Test
    public void showsAppliedSwitchesAndAboutRows() {
        attachEntry();
        clickEntry();
        Dialog menu = ShadowDialog.getLatestDialog();

        Assert.assertTrue(menu.isShowing());
        Assert.assertNotNull(description(menu.getWindow().getDecorView(), AppliedPatches.UNLOCK_PREMIUM));
        Assert.assertNotNull(description(menu.getWindow().getDecorView(), AppliedPatches.UNLOCK_CUSTOMIZATIONS));
        Assert.assertNotNull(text(menu.getWindow().getDecorView(), "Reset to defaults"));
        Assert.assertNotNull(text(menu.getWindow().getDecorView(), "App version"));
        Assert.assertNotNull(text(menu.getWindow().getDecorView(), "Patch bundle"));
    }

    @Test
    public void listsAppliedPatchesCollapsedAfterAbout() {
        attachEntry();
        clickEntry();
        View menu = ShadowDialog.getLatestDialog().getWindow().getDecorView();

        Assert.assertNotNull(text(menu, "Applied patches"));
        Assert.assertNotNull(text(menu, AppliedPatches.UNLOCK_CUSTOMIZATIONS));
        Assert.assertNotNull(text(menu, AppliedPatches.UNLOCK_PREMIUM));
        Assert.assertEquals(View.GONE,
                text(appliedCard(menu), EveryPatchApplied.CUSTOMIZATIONS_DESCRIPTION).getVisibility());
        Assert.assertEquals(View.GONE, text(appliedCard(menu), EveryPatchApplied.PREMIUM_DESCRIPTION).getVisibility());
    }

    @Test
    public void expandsAndCollapsesAppliedPatchDescription() {
        attachEntry();
        clickEntry();
        View menu = ShadowDialog.getLatestDialog().getWindow().getDecorView();
        TextView details = text(appliedCard(menu), EveryPatchApplied.PREMIUM_DESCRIPTION);
        View row = (View) details.getParent().getParent();

        row.performClick();
        Assert.assertEquals(View.VISIBLE, details.getVisibility());

        row.performClick();
        Assert.assertEquals(View.GONE, details.getVisibility());
    }

    @Test
    public void confirmsRestartAfterSwitchChange() {
        attachEntry();
        clickEntry();
        Dialog menu = ShadowDialog.getLatestDialog();
        Switch toggle = (Switch) description(menu.getWindow().getDecorView(), AppliedPatches.UNLOCK_CUSTOMIZATIONS);

        toggle.performClick();
        Dialog confirmation = ShadowDialog.getLatestDialog();

        Assert.assertFalse(PatchSettings.areCustomizationsUnlocked());
        Assert.assertNotNull(text(confirmation.getWindow().getDecorView(), "Restart required"));
        Assert
            .assertNotNull(text(confirmation.getWindow().getDecorView(), "Liftoff must restart to apply the change."));
        Assert.assertNotNull(text(confirmation.getWindow().getDecorView(), "Later"));
        Assert.assertNotNull(text(confirmation.getWindow().getDecorView(), "Restart"));
    }

    @Test
    public void usesHostColorForActiveSwitchThumb() {
        this.root.setBackgroundColor(HOST_BACKGROUND_COLOR);
        attachEntry();
        clickEntry();
        Dialog menu = ShadowDialog.getLatestDialog();
        Switch toggle = (Switch) description(menu.getWindow().getDecorView(), AppliedPatches.UNLOCK_PREMIUM);

        Assert.assertEquals(HOST_TEXT_COLOR,
                toggle.getThumbTintList().getColorForState(new int[] { android.R.attr.state_checked }, 0));
    }

    @Test
    @Config(qualifiers = "night")
    public void usesLightHostColorsInSystemDarkMode() {
        assertHostColors(LIGHT_HOST_TEXT_COLOR, LIGHT_HOST_BACKGROUND_COLOR, LIGHT_HOST_CARD_COLOR);
    }

    @Test
    @Config(qualifiers = "notnight")
    public void usesDarkHostColorsInSystemLightMode() {
        assertHostColors(HOST_TEXT_COLOR, HOST_BACKGROUND_COLOR, HOST_CARD_COLOR);
    }

    @Test
    public void readsNativeTextSpansForEntryAndMenu() {
        TextView template = text(this.root, "Restore Purchases");
        SpannableString label = new SpannableString("Restore Purchases");
        label.setSpan(new ForegroundColorSpan(LIGHT_HOST_TEXT_COLOR), 0, label.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        template.setText(label);
        attachEntry();

        TextView entry = text(this.root.findViewWithTag(this.scroll), PatchesMenu.SETTINGS_ROW_TITLE);
        SettingsStyle style = new SettingsStyle(this.activity, template);

        Assert.assertEquals(LIGHT_HOST_TEXT_COLOR, entry.getCurrentTextColor());
        Assert.assertEquals(LIGHT_HOST_TEXT_COLOR, style.primaryText);
    }

    @Test
    public void resetsSwitchesBeforeRequestingRestart() {
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS, false);
        attachEntry();
        clickEntry();
        Dialog menu = ShadowDialog.getLatestDialog();
        View resetRow = text(menu.getWindow().getDecorView(), "Reset to defaults");
        while (!resetRow.isClickable()) {
            resetRow = (View) resetRow.getParent();
        }
        resetRow.performClick();
        Dialog reset = ShadowDialog.getLatestDialog();
        Assert.assertNotNull(text(reset.getWindow().getDecorView(), "Reset to defaults?"));
        Assert.assertNotNull(text(reset.getWindow().getDecorView(), "Restores every switch to its default."));
        Assert.assertNotNull(text(reset.getWindow().getDecorView(), "Cancel"));

        text(reset.getWindow().getDecorView(), "Reset").performClick();
        Dialog restart = ShadowDialog.getLatestDialog();

        Assert.assertTrue(PatchSettings.isPremiumUnlocked());
        Assert.assertTrue(PatchSettings.areCustomizationsUnlocked());
        Assert.assertNotNull(text(restart.getWindow().getDecorView(), "Restart required"));
    }

    private void assertHostColors(int textColor, int backgroundColor, int cardColor) {
        this.root.setBackgroundColor(backgroundColor);
        TextView template = text(this.root, "Restore Purchases");
        template.setTextColor(textColor);

        SettingsStyle style = new SettingsStyle(this.activity, template);

        Assert.assertEquals(backgroundColor, style.screenBackground);
        Assert.assertEquals(cardColor, style.cardSurface);
        Assert.assertEquals(textColor, style.primaryText);
        Assert.assertEquals(backgroundColor, style.divider);
    }

    private void attachEntry() {
        PatchesMenu.onScrollLayout(this.scroll);
        ShadowLooper.idleMainLooper();
    }

    private void clickEntry() {
        TextView title = text(this.root.findViewWithTag(this.scroll), PatchesMenu.SETTINGS_ROW_TITLE);
        ((View) title.getParent()).performClick();
    }

    private void addLabel(String title) {
        TextView label = new TextView(this.activity);
        label.setText(title);
        label.setTextColor(HOST_TEXT_COLOR);
        FrameLayout row = new FrameLayout(this.activity);
        row.addView(label);
        this.content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEIGHT_PX));
    }

    private static TextView text(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                TextView found = text(group.getChildAt(index), value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View description(View view, String value) {
        if (view.getContentDescription() != null && value.contentEquals(view.getContentDescription())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                View found = description(group.getChildAt(index), value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private View appliedCard(View menu) {
        View heading = text(menu, "Applied patches");
        LinearLayout column = (LinearLayout) heading.getParent();
        return column.getChildAt(column.indexOfChild(heading) + 1);
    }

}
