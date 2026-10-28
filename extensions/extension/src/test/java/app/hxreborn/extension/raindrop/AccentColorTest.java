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
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class AccentColorTest {

    private static final byte[] PLACEHOLDER = "#1988e0".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void replacesTheOnlyPlaceholder() {
        // given
        byte[] bytes = ascii("ab#1988e0cd");

        // when
        boolean replaced = AccentColor.replaceOnce(bytes, PLACEHOLDER, "#a1b2c3");

        // then
        assertTrue(replaced);
        assertEquals("ab#a1b2c3cd", new String(bytes, StandardCharsets.US_ASCII));
    }

    @Test
    public void leavesBytesAloneWhenThePlaceholderRepeats() {
        // given
        byte[] bytes = ascii("#1988e0#1988e0");

        // when
        boolean replaced = AccentColor.replaceOnce(bytes, PLACEHOLDER, "#a1b2c3");

        // then
        assertFalse(replaced);
        assertEquals("#1988e0#1988e0", new String(bytes, StandardCharsets.US_ASCII));
    }

    @Test
    public void rejectsBytesWithoutThePlaceholder() {
        assertFalse(AccentColor.replaceOnce(ascii("#1988E0"), PLACEHOLDER, "#a1b2c3"));
    }

    @Test
    public void formatsAColorAsLowercaseHexWithoutAlpha() {
        assertEquals("#0a0b0c", AccentColor.hex(0xFF0A0B0C));
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

}
