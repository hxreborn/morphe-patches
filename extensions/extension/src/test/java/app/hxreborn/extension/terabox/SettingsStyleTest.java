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
import android.content.res.TypedArray;
import android.view.ContextThemeWrapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 })
public final class SettingsStyleTest {

    @Test
    public void followsLightHostThemeDuringSystemDarkMode() {
        RuntimeEnvironment.setQualifiers("+night");
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                android.R.style.Theme_Material_Light_NoActionBar);
        SettingsStyle style = new SettingsStyle(context);

        Assert.assertEquals(themeColor(context, android.R.attr.colorBackground), style.screenBackground);
        Assert.assertEquals(themeColor(context, android.R.attr.colorBackgroundFloating), style.cardSurface);
        Assert.assertEquals(themeColor(context, android.R.attr.textColorPrimary), style.primaryText);
        Assert.assertEquals(themeColor(context, android.R.attr.textColorSecondary), style.secondaryText);
    }

    @Test
    public void followsDarkHostThemeDuringSystemLightMode() {
        RuntimeEnvironment.setQualifiers("+notnight");
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                android.R.style.Theme_Material_NoActionBar);
        SettingsStyle style = new SettingsStyle(context);

        Assert.assertEquals(themeColor(context, android.R.attr.colorBackground), style.screenBackground);
        Assert.assertEquals(themeColor(context, android.R.attr.colorBackgroundFloating), style.cardSurface);
        Assert.assertEquals(themeColor(context, android.R.attr.textColorPrimary), style.primaryText);
        Assert.assertEquals(themeColor(context, android.R.attr.textColorSecondary), style.secondaryText);
    }

    private static int themeColor(Context context, int attribute) {
        TypedArray values = context.obtainStyledAttributes(new int[] { attribute });
        try {
            Assert.assertTrue(values.hasValue(0));
            return values.getColor(0, 0);
        } finally {
            values.recycle();
        }
    }

}
