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
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchesThemeTest {

    private static final ContextWrapper NO_THEME = new ContextWrapper(null);

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void grayBelowHalfBrightnessIsDark() {
        assertTrue(PatchesTheme.isDark(0xFF000000));
        assertTrue(PatchesTheme.isDark(0xFF7F7F7F));
        assertFalse(PatchesTheme.isDark(0xFF808080));
        assertFalse(PatchesTheme.isDark(0xFFFFFFFF));
    }

    @Test
    public void greenWeighsMoreThanTheOtherChannels() {
        assertTrue(PatchesTheme.isDark(0xFFFF0000));
        assertTrue(PatchesTheme.isDark(0xFF0000FF));
        assertFalse(PatchesTheme.isDark(0xFF00FF00));
    }

    @Test
    public void brightnessTruncatesBeforeComparing() {
        assertTrue(PatchesTheme.isDark(0xFF00DA00));
        assertFalse(PatchesTheme.isDark(0xFF00DB00));
    }

    @Test
    public void alphaDoesNotAffectDarkness() {
        assertTrue(PatchesTheme.isDark(0xFF101010));
        assertTrue(PatchesTheme.isDark(0x00101010));
        assertFalse(PatchesTheme.isDark(0x00FFFFFF));
    }

    @Test
    public void protonDarkSurfacesAreDark() {
        assertTrue(PatchesTheme.isDark(0xFF191927));
        assertTrue(PatchesTheme.isDark(0xFF2B2B38));
        assertFalse(PatchesTheme.isDark(0xFFEDEDEE));
    }

    @Test
    public void halfAlphaReplacesTheAlphaChannel() {
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0xFF123456));
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0x00123456));
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0x80123456));
        assertEquals(0x80000000, PatchesTheme.withHalfAlpha(0));
        assertEquals(0x80FFFFFF, PatchesTheme.withHalfAlpha(0xFFFFFFFF));
    }

    @Test
    public void halfAlphaIsIdempotent() {
        // given
        final int once = PatchesTheme.withHalfAlpha(0xFF9292F9);

        // when
        final int twice = PatchesTheme.withHalfAlpha(once);

        // then
        assertEquals(once, twice);
    }

}
