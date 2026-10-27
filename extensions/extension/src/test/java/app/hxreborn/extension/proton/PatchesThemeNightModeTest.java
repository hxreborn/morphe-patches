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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
public final class PatchesThemeNightModeTest {

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void followsTheSystemWhenTheAppSetsNoNightMode() {
        assertEquals(Configuration.UI_MODE_NIGHT_UNDEFINED, PatchesTheme.uiNightMode());
        assertFalse(PatchesTheme.isAppNightMode(application()));
    }

    @Test
    @Config(qualifiers = "night")
    public void followsTheNightSystemWhenTheAppSetsNoNightMode() {
        assertTrue(PatchesTheme.isNightMode(application()));
        assertTrue(PatchesTheme.isAppNightMode(application()));
    }

    @Test
    public void keepsTheContextWhenTheAppSetsNoNightMode() {
        assertSame(application(), PatchesTheme.withNightMode(application()));
        assertSame(application(), PatchesTheme.withAppNightTheme(application()));
    }

    @Test
    @Config(shadows = AlwaysNight.class)
    public void overridesALightSystemWithTheAppNightMode() {
        assertEquals(Configuration.UI_MODE_NIGHT_YES, PatchesTheme.uiNightMode());
        assertFalse(PatchesTheme.isNightMode(application()));
        assertTrue(PatchesTheme.isAppNightMode(application()));
    }

    @Test
    @Config(qualifiers = "night", shadows = AlwaysLight.class)
    public void overridesANightSystemWithTheAppLightMode() {
        assertEquals(Configuration.UI_MODE_NIGHT_NO, PatchesTheme.uiNightMode());
        assertTrue(PatchesTheme.isNightMode(application()));
        assertFalse(PatchesTheme.isAppNightMode(application()));
    }

    @Test
    @Config(shadows = AlwaysNight.class)
    public void wrapsTheContextWhenTheAppNightModeDiffers() {
        // when
        final Context themed = PatchesTheme.withAppNightTheme(application());

        // then
        assertNotSame(application(), themed);
        assertTrue(PatchesTheme.isNightMode(themed));
    }

    @Test
    @Config(qualifiers = "night", shadows = AlwaysNight.class)
    public void keepsTheContextWhenTheAppNightModeMatches() {
        assertSame(application(), PatchesTheme.withNightMode(application()));
        assertSame(application(), PatchesTheme.withAppNightTheme(application()));
    }

    @Test
    @Config(shadows = AlwaysNight.class)
    public void carriesTheAppNightModeInTheApplicationTheme() {
        assertTrue(PatchesTheme.isNightMode(PatchesTheme.withApplicationTheme(application())));
    }

    @Test
    @Config(qualifiers = "xhdpi")
    public void scalesDpByTheScreenDensity() {
        assertEquals(104, PatchesTheme.dpToPx(application(), 52));
        assertEquals(0, PatchesTheme.dpToPx(application(), 0));
    }

    @Implements(PatchesTheme.class)
    public static final class AlwaysNight {

        @Implementation
        public static int appCompatDefaultNightMode() {
            return 2;
        }

    }

    @Implements(PatchesTheme.class)
    public static final class AlwaysLight {

        @Implementation
        public static int appCompatDefaultNightMode() {
            return 1;
        }

    }

}
