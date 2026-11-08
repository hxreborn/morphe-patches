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

import android.app.Activity;
import android.content.res.TypedArray;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SettingsStyleFallbackTest {

    private static final int TITLE_COLOR = 0xFF3A3E4A;

    @Test
    public void useFallbackColorsWhenHostColorsAreMissing() {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        activity.setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        FrameLayout page = new FrameLayout(activity);
        TextView title = new TextView(activity);
        title.setTextColor(TITLE_COLOR);
        page.addView(title);

        SettingsStyle style = new SettingsStyle(activity, title);

        Assert.assertTrue(style.lightScreen);
        Assert.assertEquals(0xFFFFFFFF, style.screenBackground);
        Assert.assertEquals(0xFFF5F5F5, style.cardSurface);
        Assert.assertEquals(themeAccent(activity), style.accent);
    }

    private static int themeAccent(Activity activity) {
        TypedArray theme = activity.obtainStyledAttributes(new int[] { android.R.attr.colorAccent });
        int accent = theme.getColor(0, TITLE_COLOR);
        theme.recycle();
        return accent;
    }

}
