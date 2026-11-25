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
package app.hxreborn.extension.outlook.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ThemeColorRampTest {

    private static final int REFERENCE = 0xFF0F6CBD;

    private static final int BROWN = 0xFF8A5A2B;

    private static final int NEAR_WHITE = 0xFFF6F6F3;

    private static final int DARK_GREEN = 0xFF2B8A3E;

    private static final int CHANNEL_TOLERANCE = 2;

    private static final double MIN_CONTRAST_WITH_WHITE = 3;

    private static final double CONTRAST_TOLERANCE = 0.05;

    private static final double CONTRAST_FLARE = 0.05;

    @Test
    public void mapsTheReferenceStepToThePickedColor() {
        // given
        ThemeColorRamp ramp = ThemeColorRamp.between(REFERENCE, BROWN);

        // when
        int mapped = ramp.transform(REFERENCE);

        // then
        assertEquals(Color.red(BROWN), Color.red(mapped), CHANNEL_TOLERANCE);
        assertEquals(Color.green(BROWN), Color.green(mapped), CHANNEL_TOLERANCE);
        assertEquals(Color.blue(BROWN), Color.blue(mapped), CHANNEL_TOLERANCE);
    }

    @Test
    public void keepsTheReferenceColorsWhenThePickIsTheReference() {
        // given
        ThemeColorRamp ramp = ThemeColorRamp.between(REFERENCE, REFERENCE);

        // when
        int mapped = ramp.transform(Color.WHITE);

        // then
        assertEquals(Color.WHITE, mapped);
    }

    @Test
    public void clampsWhitePickToThreeToOneContrastWithWhiteText() {
        // given
        ThemeColorRamp ramp = ThemeColorRamp.between(REFERENCE, Color.WHITE);

        // when
        int mapped = ramp.transform(REFERENCE);

        // then
        double contrast = (Color.luminance(Color.WHITE) + CONTRAST_FLARE) / (Color.luminance(mapped) + CONTRAST_FLARE);
        assertTrue("contrast " + contrast, contrast >= MIN_CONTRAST_WITH_WHITE - CONTRAST_TOLERANCE);
    }

    @Test
    public void measuresNoDistanceBetweenEqualColors() {
        // when
        double distance = ThemeColorRamp.distance(BROWN, BROWN);

        // then
        assertEquals(0, distance, 0);
    }

    @Test
    public void nearWhiteFailsWhiteTextContrast() {
        // when
        boolean readable = ThemeColorRamp.hasWhiteTextContrast(NEAR_WHITE);

        // then
        assertFalse(readable);
    }

    @Test
    public void darkGreenPassesWhiteTextContrast() {
        // when
        boolean readable = ThemeColorRamp.hasWhiteTextContrast(DARK_GREEN);

        // then
        assertTrue(readable);
    }

}
