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

public final class HexColorTest {

    private static final int BROWN = 0xFF8A5A2B;

    @Test
    public void parsesHashPrefixedColor() {
        // when
        int color = HexColor.parse("#8A5A2B");

        // then
        assertEquals(BROWN, color);
    }

    @Test
    public void parsesLowercaseColorWithoutHash() {
        // when
        int color = HexColor.parse(" 8a5a2b ");

        // then
        assertEquals(BROWN, color);
    }

    @Test
    public void rejectsShortColor() {
        // when
        int color = HexColor.parse("#12");

        // then
        assertEquals(HexColor.INVALID, color);
    }

    @Test
    public void rejectsNonHexDigits() {
        // when
        int color = HexColor.parse("#8A5A2G");

        // then
        assertEquals(HexColor.INVALID, color);
    }

    @Test
    public void formatsUppercaseWithoutAlpha() {
        // when
        String text = HexColor.format(BROWN);

        // then
        assertEquals("#8A5A2B", text);
    }

}
