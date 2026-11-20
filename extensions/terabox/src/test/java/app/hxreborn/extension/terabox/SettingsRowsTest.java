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

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 }, shadows = FakeHostResources.class)
public final class SettingsRowsTest {

    private static final int CONTROL_TOUCH_TARGET_DP = 48;

    private static final String TITLE = "Remove recent panel";

    private static final String DESCRIPTION = "Removes Recent, Starred and Offline from Home.";

    @Test
    public void exposesOneAccessibleSwitchAndFullRowTouchTarget() {
        Context context = RuntimeEnvironment.getApplication();
        SettingsStyle style = new SettingsStyle(context);
        SettingsRows rows = new SettingsRows(context, style);
        LinearLayout card = rows.card();
        rows.switchRow(card, "", TITLE, DESCRIPTION, true, (button, enabled) -> {
        });
        LinearLayout row = (LinearLayout) card.getChildAt(0);
        CheckBox control = (CheckBox) row.getChildAt(row.getChildCount() - 1);

        Assert.assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, row.getImportantForAccessibility());
        Assert.assertEquals(TITLE + ". " + DESCRIPTION, control.getContentDescription());
        Assert.assertTrue(control.getMinWidth() >= style.dp(CONTROL_TOUCH_TARGET_DP));
        Assert.assertTrue(control.getMinHeight() >= style.dp(CONTROL_TOUCH_TARGET_DP));
        Assert.assertTrue(control.isChecked());
        row.performClick();
        Assert.assertFalse(control.isChecked());
    }

    @Test
    public void expandsAndCollapsesDescriptionOnRowClick() {
        Context context = RuntimeEnvironment.getApplication();
        SettingsRows rows = new SettingsRows(context, new SettingsStyle(context));
        LinearLayout card = rows.card();
        rows.expandableRow(card, TITLE, DESCRIPTION);
        LinearLayout row = (LinearLayout) card.getChildAt(0);
        LinearLayout labels = (LinearLayout) row.getChildAt(0);
        TextView description = (TextView) labels.getChildAt(1);

        Assert.assertEquals(TITLE, ((TextView) labels.getChildAt(0)).getText().toString());
        Assert.assertEquals(DESCRIPTION, description.getText().toString());
        Assert.assertEquals(View.GONE, description.getVisibility());
        row.performClick();
        Assert.assertEquals(View.VISIBLE, description.getVisibility());
        row.performClick();
        Assert.assertEquals(View.GONE, description.getVisibility());
    }

    @Test
    public void separatesExpandableRowsWithADivider() {
        Context context = RuntimeEnvironment.getApplication();
        SettingsRows rows = new SettingsRows(context, new SettingsStyle(context));
        LinearLayout card = rows.card();
        rows.expandableRow(card, TITLE, DESCRIPTION);
        rows.expandableRow(card, "Hide ads", "Removes ads.");

        Assert.assertEquals(3, card.getChildCount());
    }

}
