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
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;

import android.view.View.MeasureSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ColumnLayoutTest {

    @Test
    public void capsAWideParent() {
        // when
        int width = measuredWidth(1600);

        // then
        assertEquals(600, width);
    }

    @Test
    public void fillsANarrowParent() {
        // when
        int width = measuredWidth(400);

        // then
        assertEquals(400, width);
    }

    private static int measuredWidth(int parentWidth) {
        ColumnLayout column = new ColumnLayout(RuntimeEnvironment.getApplication(), 600);
        column.measure(MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST));
        return column.getMeasuredWidth();
    }

}
