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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class HsvColorTest {

    private static final float GREEN_HUE = 120f;

    private static final float SATURATION = 0.8f;

    private static final float FULL = 1f;

    private static final float DELTA = 0.001f;

    private static final String[] HEX_COLORS = { "#2B8A3E", "#E3008C", "#0F6CBD", "#8A5A2B", "#000000", "#FFFFFF" };

    @Test
    public void convertsHueSaturationAndValueToHex() {
        // given
        HsvColor color = new HsvColor();
        color.setHueAndSaturation(GREEN_HUE, SATURATION);
        color.setValue(FULL);

        // when
        String hex = HexColor.format(color.toColor());

        // then
        assertEquals("#33FF33", hex);
    }

    @Test
    public void keepsHexThroughHsvRoundTrip() {
        for (String hex : HEX_COLORS) {
            // given
            HsvColor color = new HsvColor();

            // when
            color.set(HexColor.parse(hex));

            // then
            assertEquals(hex, HexColor.format(color.toColor()));
        }
    }

    @Test
    public void readsHueSaturationAndValueFromHex() {
        // given
        HsvColor color = new HsvColor();

        // when
        color.set(HexColor.parse("#33FF33"));

        // then
        assertEquals(GREEN_HUE, color.hue(), DELTA);
        assertEquals(SATURATION, color.saturation(), DELTA);
        assertEquals(FULL, color.value(), DELTA);
    }

    @Test
    public void fullValueColorKeepsHueAndSaturation() {
        // given
        HsvColor color = new HsvColor();
        color.set(HexColor.parse("#339933"));

        // when
        String hex = HexColor.format(color.toFullValueColor());

        // then
        assertEquals("#55FF55", hex);
    }

    @Test
    public void wrapsNegativeHueIntoOneTurn() {
        // given
        HsvColor color = new HsvColor();

        // when
        color.setHueAndSaturation(-GREEN_HUE, SATURATION);

        // then
        assertEquals(240f, color.hue(), DELTA);
    }

    @Test
    public void clampsSaturationAndValueToOne() {
        // given
        HsvColor color = new HsvColor();

        // when
        color.setHueAndSaturation(GREEN_HUE, 2f);
        color.setValue(-1f);

        // then
        assertEquals(FULL, color.saturation(), DELTA);
        assertEquals(0f, color.value(), DELTA);
    }

}
