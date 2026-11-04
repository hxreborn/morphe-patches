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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SettingsStyleTest {

    @After
    public void restoreSystemLightMode() {
        RuntimeEnvironment.setQualifiers("+notnight");
    }

    @Test
    public void preserveHostPaletteWhenSystemDarkModeChanges() {
        RuntimeEnvironment.setQualifiers("+notnight");
        int[] light = hostPalette(0xFFF3F3F3, 0xFFFFFFFF);
        RuntimeEnvironment.setQualifiers("+night");
        int[] dark = hostPalette(0xFFF3F3F3, 0xFFFFFFFF);

        Assert.assertArrayEquals(light, dark);
    }

    @Test
    public void takeSurfacesFromHostPageInLightTheme() {
        SettingsStyle style = styleOn(0xFFF3F3F3, 0xFFFFFFFF, 0xFF3A3E4A);

        Assert.assertTrue(style.lightScreen);
        Assert.assertEquals(0xFFFFFFFF, style.screenBackground);
        Assert.assertEquals(0xFFF3F3F3, style.cardSurface);
    }

    @Test
    public void takeSurfacesFromHostPageInDarkTheme() {
        SettingsStyle style = styleOn(0xFF121212, 0xFF1F1F1F, 0xFFEEEEEE);

        Assert.assertFalse(style.lightScreen);
        Assert.assertEquals(0xFF121212, style.screenBackground);
        Assert.assertEquals(0xFF1F1F1F, style.cardSurface);
    }

    @Test
    public void deriveTextAndDividerFromHostTitleColor() {
        SettingsStyle style = styleOn(0xFF121212, 0xFF1F1F1F, 0xFFEEEEEE);

        Assert.assertEquals(0xFFEEEEEE, style.primaryText);
        Assert.assertEquals(0xB3EEEEEE, style.secondaryText);
        Assert.assertEquals(0x1FEEEEEE, style.divider);
    }

    private static int[] hostPalette(int page, int row) {
        SettingsStyle style = styleOn(page, row, 0xFF3A3E4A);
        return new int[] { style.screenBackground, style.cardSurface, style.divider, style.primaryText,
                style.secondaryText, style.accent };
    }

    private static SettingsStyle styleOn(int page, int row, int titleColor) {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        activity.setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        FrameLayout pageView = new FrameLayout(activity);
        pageView.setBackgroundColor(page);
        LinearLayout section = new LinearLayout(activity);
        section.setBackgroundColor(row);
        pageView.addView(section);
        TextView title = new TextView(activity);
        title.setTextColor(titleColor);
        section.addView(title);
        return new SettingsStyle(activity, title);
    }

}
