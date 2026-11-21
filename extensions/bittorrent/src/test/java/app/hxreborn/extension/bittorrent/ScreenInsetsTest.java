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
package app.hxreborn.extension.bittorrent;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowInsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ScreenInsetsTest {

    private static final int STATUS_BAR_HEIGHT = 84;

    private static final int CUTOUT_HEIGHT = 204;

    private static final int NAVIGATION_BAR_HEIGHT = 72;

    private static final int SIDE_CUTOUT_WIDTH = 120;

    @Test
    public void applyLargerCutoutWithoutAddingPaddingOnRedispatch() {
        View view = new View(RuntimeEnvironment.getApplication());
        ScreenInsets listener = new ScreenInsets(true);
        WindowInsets insets = new WindowInsets.Builder()
            .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, STATUS_BAR_HEIGHT, 0, 0))
            .setInsets(WindowInsets.Type.displayCutout(), Insets.of(SIDE_CUTOUT_WIDTH, CUTOUT_HEIGHT, 0, 0))
            .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR_HEIGHT))
            .build();

        listener.onApplyWindowInsets(view, insets);
        WindowInsets remaining = listener.onApplyWindowInsets(view, insets);

        assertEquals(CUTOUT_HEIGHT, view.getPaddingTop());
        assertEquals(SIDE_CUTOUT_WIDTH, view.getPaddingLeft());
        assertEquals(NAVIGATION_BAR_HEIGHT, view.getPaddingBottom());
        assertEquals(Insets.NONE,
                remaining.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()));
    }

    @Test
    public void replaceFixedPageSpacingAndPaintInsetAreaInBothThemes() {
        View main = new View(RuntimeEnvironment.getApplication());
        View page = new View(RuntimeEnvironment.getApplication());
        for (int color : new int[] { Color.BLACK, Color.WHITE }) {
            page.setBackgroundColor(color);
            page.setPadding(0, STATUS_BAR_HEIGHT, 0, NAVIGATION_BAR_HEIGHT);

            ScreenInsets.clearPageSpacing(page, main);

            assertEquals(0, page.getPaddingTop());
            assertEquals(NAVIGATION_BAR_HEIGHT, page.getPaddingBottom());
            assertEquals(color, ((ColorDrawable) main.getBackground()).getColor());
        }
    }

    @Test
    public void leaveNavigationInsetForHostTabs() {
        View view = new View(RuntimeEnvironment.getApplication());
        WindowInsets insets = new WindowInsets.Builder()
            .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR_HEIGHT))
            .build();

        WindowInsets remaining = new ScreenInsets(false).onApplyWindowInsets(view, insets);

        assertEquals(0, view.getPaddingBottom());
        assertEquals(NAVIGATION_BAR_HEIGHT, remaining.getInsets(WindowInsets.Type.navigationBars()).bottom);
    }

    @Test
    @SuppressWarnings("resource")
    public void preserveContentOnAndroid16() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        View view = new View(activity);
        view.setPadding(SIDE_CUTOUT_WIDTH, STATUS_BAR_HEIGHT, 0, NAVIGATION_BAR_HEIGHT);
        activity.setContentView(view);

        ScreenInsets.attach(activity);
        ScreenInsets.attachSheet(view);

        assertEquals(STATUS_BAR_HEIGHT, view.getPaddingTop());
        assertEquals(SIDE_CUTOUT_WIDTH, view.getPaddingLeft());
        assertEquals(NAVIGATION_BAR_HEIGHT, view.getPaddingBottom());
    }

}
