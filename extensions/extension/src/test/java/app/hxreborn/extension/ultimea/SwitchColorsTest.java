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
package app.hxreborn.extension.ultimea;

import org.junit.Assert;
import org.junit.Test;

public class SwitchColorsTest {

    private static final double MINIMUM_CONTRAST = 3;

    private static final int LIGHT_SURFACE = 0xFFFFFFFF;

    private static final int DARK_SURFACE = 0xFF1E1E1E;

    private static final int AMOLED_SURFACE = 0xFF000000;

    private static final int[] ACCENTS = { 0xFF541597, 0xFFC499FF, 0xFF006A6A, 0xFF4CDADA, 0xFF8B5000, 0xFFFFB95C,
            0xFFFFFFFF, 0xFF000000 };

    @Test
    public void everyLightStateMeetsMinimumContrast() {
        assertEveryState(LIGHT_SURFACE, false);
    }

    @Test
    public void everyDarkStateMeetsMinimumContrast() {
        assertEveryState(DARK_SURFACE, true);
    }

    @Test
    public void everyAmoledStateMeetsMinimumContrast() {
        assertEveryState(AMOLED_SURFACE, true);
    }

    private static void assertEveryState(int surface, boolean dark) {
        for (int accent : ACCENTS) {
            SwitchColors colors = new SwitchColors(accent, surface, dark);
            assertAtLeastMinimum("checked track", colors.checkedTrack, surface, accent);
            assertAtLeastMinimum("checked thumb", colors.thumb, colors.checkedTrack, accent);
            assertAtLeastMinimum("unchecked track", colors.uncheckedTrack, surface, accent);
            assertAtLeastMinimum("unchecked thumb", colors.thumb, colors.uncheckedTrack, accent);
        }
    }

    private static void assertAtLeastMinimum(String part, int foreground, int background, int accent) {
        double contrast = contrast(foreground, background);
        Assert.assertTrue(
                part + " " + Integer.toHexString(foreground) + " on " + Integer.toHexString(background)
                        + " with accent " + Integer.toHexString(accent) + " is " + contrast,
                contrast >= MINIMUM_CONTRAST);
    }

    private static double contrast(int first, int second) {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05) / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double luminance(int color) {
        double[] weights = { 0.2126, 0.7152, 0.0722 };
        double luminance = 0;
        for (int channel = 0; channel < weights.length; channel++) {
            double srgb = ((color >>> (16 - channel * 8)) & 255) / 255.0;
            double linear = (srgb <= 0.04045) ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
            luminance += weights[channel] * linear;
        }
        return luminance;
    }

}
