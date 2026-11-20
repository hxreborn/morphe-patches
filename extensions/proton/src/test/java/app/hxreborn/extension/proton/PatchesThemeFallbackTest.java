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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchesThemeFallbackTest {

    private static final ContextWrapper NO_THEME = new ContextWrapper(null);

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void everyColorFallsBackToItsOwnProtonDarkColor() {
        for (Context context : new Context[] { NO_THEME, application() }) {
            assertEquals(0xFF1C1B24, PatchesTheme.backgroundNorm(context));
            assertEquals(0xFF292733, PatchesTheme.backgroundSecondary(context));
            assertEquals(0xFFEDEDEE, PatchesTheme.textNorm(context));
            assertEquals(0xFFA9A9AF, PatchesTheme.textWeak(context));
            assertEquals(0xFF5B5966, PatchesTheme.iconDisabled(context));
        }
    }

    @Test
    public void fallbackColorsAreDistinctAndOpaque() {
        final Context context = application();
        final Set<Integer> colors = new HashSet<>();
        colors.add(PatchesTheme.backgroundNorm(context));
        colors.add(PatchesTheme.backgroundSecondary(context));
        colors.add(PatchesTheme.textNorm(context));
        colors.add(PatchesTheme.textWeak(context));
        colors.add(PatchesTheme.iconDisabled(context));

        assertEquals(5, colors.size());
        for (int color : colors) {
            assertEquals(0xFF, color >>> 24);
        }
    }

    @Test
    public void textColorDiffersFromBackgroundColor() {
        final Context context = application();

        assertTrue(PatchesTheme.textNorm(context) != PatchesTheme.backgroundNorm(context));
    }

    @Test
    public void missingDrawableIdGivesNoDrawable() {
        final Context context = application();

        assertNull(PatchesTheme.drawable(context, PatchesTheme.backArrowDrawable()));
        assertNull(PatchesTheme.drawable(context, PatchesTheme.chevronRightDrawable()));
        assertNull(PatchesTheme.drawable(context, PatchesTheme.checkmarkDrawable()));
    }

}
