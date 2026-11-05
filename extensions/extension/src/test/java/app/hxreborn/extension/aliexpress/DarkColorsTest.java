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

import org.junit.Assert;
import org.junit.Test;

public class DarkColorsTest {

    @Test
    public void mapsWhiteBackgroundToCardSurface() {
        Assert.assertEquals(0xFF1E1E1E, DarkColors.night(DarkColors.BACKGROUND_TYPE, 0xFFFFFFFF));
    }

    @Test
    public void mapsLightGreyBackgroundToPageSurface() {
        Assert.assertEquals(0xFF121212, DarkColors.night(DarkColors.BACKGROUND_TYPE, 0xFFF5F5F5));
    }

    @Test
    public void keepsBrandColourBackground() {
        Assert.assertEquals(0xFFFD384F, DarkColors.night(DarkColors.BACKGROUND_TYPE, 0xFFFD384F));
    }

    @Test
    public void keepsDarkBackground() {
        Assert.assertEquals(0xFF191919, DarkColors.night(DarkColors.BACKGROUND_TYPE, 0xFF191919));
    }

    @Test
    public void mapsNearBlackTextToLightText() {
        Assert.assertEquals(0xFFE8E8E8, DarkColors.night(DarkColors.TEXT_TYPE, 0xFF191919));
    }

    @Test
    public void mapsMidGreyTextToSecondaryText() {
        Assert.assertEquals(0xFFB8B8B8, DarkColors.night(DarkColors.TEXT_TYPE, 0xFF606472));
    }

    @Test
    public void keepsWhiteText() {
        Assert.assertEquals(0xFFFFFFFF, DarkColors.night(DarkColors.TEXT_TYPE, 0xFFFFFFFF));
    }

    @Test
    public void keepsAlphaOfTranslucentBackground() {
        Assert.assertEquals(0x801E1E1E, DarkColors.night(DarkColors.BACKGROUND_TYPE, 0x80FFFFFF));
    }

    @Test
    public void mapsLightDividerToDarkDivider() {
        Assert.assertEquals(0xFF3A3A3A, DarkColors.night(DarkColors.LINE_TYPE, 0xFFEBEBEB));
    }

    @Test
    public void keepsColourOfUnknownType() {
        Assert.assertEquals(0xFFFFFFFF, DarkColors.night(3, 0xFFFFFFFF));
    }

}
