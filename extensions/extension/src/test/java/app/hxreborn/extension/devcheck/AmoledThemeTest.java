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
package app.hxreborn.extension.devcheck;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AmoledThemeTest {

    private static final int THEME_LIST_ID = 0x7f010001;

    private static final int PREFERENCE_ID = 0x7f010002;

    private static final int TITLE_ID = 0x7f020001;

    private static final String TITLE = "Prefer pure black in dark theme";

    @After
    public void clearPreference() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("hx_devcheck_patches", Activity.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void keepStockThemeByDefault() {
        ThemeActivity activity = activity();
        Drawable original = new ColorDrawable(Color.MAGENTA);
        activity.getWindow().setBackgroundDrawable(original);
        AmoledTheme.applyOverlay(activity);
        Assert.assertFalse(PatchSettings.isAmoled(activity));
        Assert.assertSame(original, activity.getWindow().getDecorView().getBackground());
    }

    @Test
    public void addOneCheckableFooterBelowThemeChoices() {
        ThemeActivity activity = activity();
        Dialog dialog = themeDialog(activity);
        AmoledTheme.addPreference(dialog);
        AmoledTheme.addPreference(dialog);
        ListView list = dialog.findViewById(THEME_LIST_ID);
        CheckedTextView preference = dialog.findViewById(PREFERENCE_ID);
        Assert.assertEquals(1, list.getFooterViewsCount());
        Assert.assertEquals(4, list.getCount());
        Assert.assertEquals("Use system theme", list.getItemAtPosition(0));
        Assert.assertEquals("Light theme", list.getItemAtPosition(1));
        Assert.assertEquals("Dark theme", list.getItemAtPosition(2));
        Assert.assertTrue(list.isItemChecked(2));
        Assert.assertEquals(TITLE, preference.getText());
        Assert.assertFalse(preference.isChecked());
        Assert.assertTrue(preference.isEnabled());
        Assert.assertFalse(list.getAdapter().isEnabled(list.getCount() - 1));
        Assert.assertNotNull(preference.getCompoundDrawablesRelative()[0]);
        dialog.dismiss();
    }

    @Test
    public void persistToggleAndRecreateActivity() {
        ThemeActivity activity = activity();
        Dialog dialog = themeDialog(activity);
        AmoledTheme.addPreference(dialog);
        CheckedTextView preference = dialog.findViewById(PREFERENCE_ID);
        Assert.assertTrue(preference.performClick());
        Assert.assertTrue(PatchSettings.isAmoled(activity));
        Assert.assertTrue(preference.isChecked());
        Assert.assertFalse(dialog.isShowing());
        Assert.assertEquals(1, activity.recreations);
        ThemeActivity reopened = activity();
        Dialog reopenedDialog = themeDialog(reopened);
        AmoledTheme.addPreference(reopenedDialog);
        CheckedTextView reopenedPreference = reopenedDialog.findViewById(PREFERENCE_ID);
        Assert.assertTrue(reopenedPreference.isChecked());
        Assert.assertTrue(reopenedPreference.performClick());
        Assert.assertFalse(PatchSettings.isAmoled(activity));
    }

    @Test
    public void applyBlackBackgroundWhenEnabledInDarkTheme() {
        ThemeActivity activity = activity();
        activity.setTheme(android.R.style.Theme_Material);
        TypedValue originalThemeBackground = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.colorBackground, originalThemeBackground, true);
        PatchSettings.setAmoled(activity, true);
        AmoledTheme.applyOverlay(activity);
        TypedValue overlayThemeBackground = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.colorBackground, overlayThemeBackground, true);
        Assert.assertNotEquals(originalThemeBackground.data, overlayThemeBackground.data);
        Drawable background = activity.getWindow().getDecorView().getBackground();
        Assert.assertTrue(background instanceof ColorDrawable);
        Assert.assertEquals(Color.BLACK, ((ColorDrawable) background).getColor());
    }

    @Test
    public void keepLightThemeWhenPreferenceIsEnabled() {
        ThemeActivity activity = activity();
        activity.setTheme(android.R.style.Theme_Material_Light_Dialog_Alert);
        Drawable original = new ColorDrawable(Color.WHITE);
        activity.getWindow().setBackgroundDrawable(original);
        PatchSettings.setAmoled(activity, true);
        AmoledTheme.applyOverlay(activity);
        Assert.assertSame(original, activity.getWindow().getDecorView().getBackground());
    }

    @Test
    public void leaveDialogsWithoutThemeListUnchanged() {
        ThemeActivity activity = activity();
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(new TextView(activity));
        dialog.show();
        AmoledTheme.addPreference(dialog);
        Assert.assertNull(dialog.findViewById(PREFERENCE_ID));
        dialog.dismiss();
    }

    @Test
    public void leaveNonCheckableListsUnchanged() {
        ThemeActivity activity = activity();
        Dialog dialog = new Dialog(activity);
        ListView list = new ListView(activity);
        list.setId(THEME_LIST_ID);
        list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                Arrays.asList("Use system theme", "Light theme", "Dark theme")));
        dialog.setContentView(list);
        dialog.show();
        AmoledTheme.addPreference(dialog);
        Assert.assertEquals(0, list.getFooterViewsCount());
        dialog.dismiss();
    }

    private static ThemeActivity activity() {
        return Robolectric.buildActivity(ThemeActivity.class).setup().get();
    }

    private static Dialog themeDialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        ListView list = new ListView(activity);
        list.setId(THEME_LIST_ID);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setAdapter(new ArrayAdapter<>(dialog.getContext(), android.R.layout.simple_list_item_single_choice,
                Arrays.asList("Use system theme", "Light theme", "Dark theme")));
        list.setItemChecked(2, true);
        dialog.setContentView(list);
        dialog.show();
        return dialog;
    }

    public static final class ThemeActivity extends Activity {

        private Resources themeResources;

        private int recreations;

        @Override
        public Resources getResources() {
            if (this.themeResources == null) {
                this.themeResources = new ThemeResources(super.getResources());
            }
            return this.themeResources;
        }

        @Override
        public void recreate() {
            this.recreations++;
        }

    }

    private static final class ThemeResources extends Resources {

        ThemeResources(Resources resources) {
            super(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
        }

        @Override
        public int getIdentifier(String name, String type, String packageName) {
            switch (name) {
                case "select_dialog_listview":
                    return THEME_LIST_ID;
                case "hx_amoled_preference":
                    return PREFERENCE_ID;
                case "hx_amoled_preference_title":
                    return TITLE_ID;
                case "isLightTheme":
                    return android.R.attr.windowIsFloating;
                case "hx_amoled_theme_overlay":
                    return android.R.style.Theme_Material_Light;
                default:
                    return super.getIdentifier(name, type, packageName);
            }
        }

        @Override
        public CharSequence getText(int id) {
            return (id == TITLE_ID) ? TITLE : super.getText(id);
        }

    }

}
