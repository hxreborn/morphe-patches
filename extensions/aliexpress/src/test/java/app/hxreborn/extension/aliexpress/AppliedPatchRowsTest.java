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

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 }, manifest = Config.NONE, shadows = FakeHostColors.class)
public final class AppliedPatchRowsTest {

    private static final String TITLE = "Applied feature";

    private static final String DESCRIPTION = "Explains the selected feature. Includes its full caveat on another line. Needs no shortened text.";

    private static final float EXPANDED_ROTATION = 180f;

    @Test
    public void expandDescriptionWithAccessibleState() {
        LinearLayout row = row(DESCRIPTION);
        TextView details = (TextView) ((LinearLayout) row.getChildAt(0)).getChildAt(1);
        ImageView chevron = (ImageView) row.getChildAt(1);
        Assert.assertEquals(View.GONE, details.getVisibility());
        Assert.assertEquals(0f, chevron.getRotation(), 0f);
        Assert.assertEquals(TITLE + ". Collapsed.", row.getContentDescription());

        row.performClick();

        Assert.assertEquals(View.VISIBLE, details.getVisibility());
        Assert.assertEquals(DESCRIPTION, details.getText().toString());
        Assert.assertNull(details.getEllipsize());
        Assert.assertEquals(EXPANDED_ROTATION, chevron.getRotation(), 0f);
        Assert.assertEquals(TITLE + ". Expanded. " + DESCRIPTION, row.getContentDescription());
    }

    @Test
    public void collapseDescriptionWithAccessibleState() {
        LinearLayout row = row(DESCRIPTION);
        row.performClick();

        row.performClick();

        Assert.assertEquals(View.GONE, ((TextView) ((LinearLayout) row.getChildAt(0)).getChildAt(1)).getVisibility());
        Assert.assertEquals(0f, ((ImageView) row.getChildAt(1)).getRotation(), 0f);
        Assert.assertEquals(TITLE + ". Collapsed.", row.getContentDescription());
    }

    @Test
    public void omitExpansionWhenDescriptionIsMissing() {
        for (String description : new String[] { null, "" }) {
            LinearLayout row = row(description);
            Assert.assertFalse(row.isClickable());
            Assert.assertFalse(row.performClick());
            Assert.assertEquals(View.GONE, ((ImageView) row.getChildAt(1)).getVisibility());
            Assert.assertEquals(TITLE, row.getContentDescription());
        }
    }

    private LinearLayout row(String description) {
        Context context = RuntimeEnvironment.getApplication();
        SettingsRows rows = new SettingsRows(context, new SettingsStyle(context, new TextView(context)));
        LinearLayout card = rows.card();
        rows.expandableRow(card, TITLE, description);
        return (LinearLayout) card.getChildAt(0);
    }

}
