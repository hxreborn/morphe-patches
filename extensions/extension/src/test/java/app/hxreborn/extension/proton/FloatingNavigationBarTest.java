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

import android.content.Context;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
public final class FloatingNavigationBarTest {

    private static final long BACKGROUND = 0xff10203000000000L;

    private static final long MAIL_DARK_SURFACE = 0xff2b2b3800000000L;

    private static final long WIDE_GAMUT_BACKGROUND = 0x123456789abcdef1L;

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @Test
    @Config(shadows = Patched.class)
    public void retainStockNavigationByDefault() {
        Object original = new Object();
        Assert.assertFalse(FloatingNavigationBar.isEnabled());
        Assert.assertSame(original, FloatingNavigationBar.modifier(original, null));
        Assert.assertEquals(BACKGROUND, FloatingNavigationBar.background(BACKGROUND));
    }

    @Test
    @Config(shadows = Patched.class)
    public void ignoreRetiredGlassSetting() {
        PatchSettings.setEnabled("glass_navigation_bar", true);
        Assert.assertEquals(BACKGROUND, FloatingNavigationBar.background(BACKGROUND));
    }

    @Test
    @Config(shadows = Patched.class)
    public void useOpaqueMailSurface() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        Assert.assertEquals(MAIL_DARK_SURFACE, FloatingNavigationBar.background(BACKGROUND));
    }

    @Test
    @Config(shadows = Patched.class)
    public void retainOpaqueMailSurfaceWithRetiredGlassSetting() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        PatchSettings.setEnabled("glass_navigation_bar", true);
        Assert.assertEquals(MAIL_DARK_SURFACE, FloatingNavigationBar.background(BACKGROUND));
    }

    @Test
    @Config(shadows = Patched.class)
    public void retainUnsupportedColorEncoding() {
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        PatchSettings.setEnabled("glass_navigation_bar", true);
        Assert.assertEquals(WIDE_GAMUT_BACKGROUND, FloatingNavigationBar.background(WIDE_GAMUT_BACKGROUND));
    }

    @Test
    @Config(shadows = Patched.class)
    public void reserveInsetsOutsideVisibleBar() {
        Assert.assertEquals(8f, FloatingNavigationBar.elevation(8f), 0f);
        Assert.assertEquals(80f, FloatingNavigationBar.height(80f), 0f);
        Assert.assertEquals(24f, FloatingNavigationBar.itemBottomPadding(24f), 0f);
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        Assert.assertEquals(0f, FloatingNavigationBar.elevation(8f), 0f);
        Assert.assertEquals(56f, FloatingNavigationBar.height(80f), 0f);
        Assert.assertEquals(0f, FloatingNavigationBar.itemBottomPadding(24f), 0f);
        Assert.assertEquals(0xffffffff00000000L, FloatingNavigationBar.background(0xfffafafa00000000L));
    }

    @After
    public void clearPreferences() {
        PatchedBuild.useApplicationContext()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void ignoreSettingsWhenPatchIsAbsent() {
        Object original = new Object();
        PatchSettings.setEnabled(FloatingNavigationBar.KEY, true);
        PatchSettings.setEnabled("glass_navigation_bar", true);
        Assert.assertFalse(FloatingNavigationBar.isEnabled());
        Assert.assertSame(original, FloatingNavigationBar.modifier(original, null));
        Assert.assertEquals(BACKGROUND, FloatingNavigationBar.background(BACKGROUND));
    }

    @Implements(FloatingNavigationBar.class)
    public static final class Patched {

        @Implementation
        protected static boolean isPatched() {
            return true;
        }

    }

}
