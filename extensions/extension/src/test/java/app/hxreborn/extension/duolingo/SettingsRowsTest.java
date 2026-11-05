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
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.Switch;
import app.morphe.extension.shared.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SettingsRowsTest {

    private static final String KEY = AppliedPatches.UNLOCK_PREMIUM;

    private SettingsRows rows;

    private int changes;

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        this.rows = new SettingsRows(new SettingsStyle(RuntimeEnvironment.getApplication()));
    }

    private View addSwitchRow(LinearLayout card) {
        this.rows.switchRow(card, "hx_duolingo_ic_premium", KEY, KEY, "Unlocks Super and keeps energy full.",
                () -> this.changes++);
        return card.getChildAt(card.getChildCount() - 1);
    }

    @Test
    public void storesNewStateAndReportsChangeWhenSwitchRowTapped() {
        // given
        View row = addSwitchRow(this.rows.card());

        // when
        row.performClick();

        // then
        assertFalse(PatchSettings.isEnabled(KEY, true));
        assertEquals(1, this.changes);
    }

    @Test
    public void describesSwitchRowAsCheckedSwitch() {
        View row = addSwitchRow(this.rows.card());

        AccessibilityNodeInfo info = row.createAccessibilityNodeInfo();

        assertEquals(Switch.class.getName(), info.getClassName());
        assertTrue(info.isCheckable());
        assertTrue(info.isChecked());
    }

    @Test
    public void appendsDividerAndValueRowAfterSwitchRow() {
        LinearLayout card = this.rows.card();

        addSwitchRow(card);
        this.rows.valueRow(card, "hx_duolingo_ic_info", "Patch bundle", "1.70.0");

        assertEquals(3, card.getChildCount());
        assertEquals("Patch bundle, 1.70.0", card.getChildAt(2).getContentDescription());
    }

}
