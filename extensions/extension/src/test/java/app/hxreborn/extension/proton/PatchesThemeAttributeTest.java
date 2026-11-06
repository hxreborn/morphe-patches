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
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = PatchesThemeAttributeTest.ThemeAttributes.class)
public final class PatchesThemeAttributeTest {

    @Implements(value = PatchesTheme.class, isInAndroidSdk = false)
    public static final class ThemeAttributes {

        @Implementation
        public static int textNormAttribute() {
            return android.R.attr.colorAccent;
        }

        @Implementation
        public static int textWeakAttribute() {
            return android.R.attr.textColorPrimary;
        }

    }

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    private static int themeColor(Context context, int attribute) {
        final TypedArray attributes = context.obtainStyledAttributes(new int[] { attribute });
        try {
            return attributes.getColor(0, 0);
        } finally {
            attributes.recycle();
        }
    }

    @Test
    public void colorAttributeOfTheThemeIsResolved() {
        // given
        final Context context = application();
        final int expected = themeColor(context, android.R.attr.colorAccent);

        // when
        final int resolved = PatchesTheme.textNorm(context);

        // then
        assertNotEquals(0xFFEDEDEE, expected);
        assertEquals(expected, resolved);
    }

    @Test
    public void colorResourceAttributeOfTheThemeIsResolved() {
        // given
        final Context context = application();
        final TypedValue value = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, value, true));
        assertTrue(value.resourceId != 0);
        assertNotEquals(0xFFA9A9AF, context.getColor(value.resourceId));

        // when
        final int resolved = PatchesTheme.textWeak(context);

        // then
        assertEquals(context.getColor(value.resourceId), resolved);
    }

}
