/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
