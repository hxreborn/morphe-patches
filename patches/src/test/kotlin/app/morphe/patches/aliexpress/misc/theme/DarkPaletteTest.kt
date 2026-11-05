/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DarkPaletteTest {

    private fun night(role: ColorRole, hex: String) = DarkPalette.night(role, checkNotNull(DarkPalette.parse(hex)))

    @Test
    fun `maps a white background to the card surface`() {
        assertEquals(0xFF1E1E1E.toInt(), night(ColorRole.BACKGROUND, "#ffffff"))
    }

    @Test
    fun `maps a light grey background to the page surface`() {
        assertEquals(0xFF121212.toInt(), night(ColorRole.BACKGROUND, "#f5f5f5"))
    }

    @Test
    fun `keeps a dark background`() {
        assertNull(night(ColorRole.BACKGROUND, "#191919"))
    }

    @Test
    fun `keeps a brand colour background`() {
        assertNull(night(ColorRole.BACKGROUND, "#fd384f"))
    }

    @Test
    fun `maps near black text to light text`() {
        assertEquals(0xFFE8E8E8.toInt(), night(ColorRole.FOREGROUND, "#191919"))
    }

    @Test
    fun `maps mid grey text to secondary text`() {
        assertEquals(0xFFB8B8B8.toInt(), night(ColorRole.FOREGROUND, "#606472"))
    }

    @Test
    fun `keeps white text`() {
        assertNull(night(ColorRole.FOREGROUND, "#ffffff"))
    }

    @Test
    fun `keeps the alpha of a translucent colour`() {
        assertEquals(0x801E1E1E.toInt(), night(ColorRole.BACKGROUND, "#80ffffff"))
    }

    @Test
    fun `maps a light divider to a dark divider`() {
        assertEquals(0xFF3A3A3A.toInt(), night(ColorRole.LINE, "#ebebeb"))
    }

    @Test
    fun `expands short hex colours`() {
        assertEquals(0xFF222222.toInt(), DarkPalette.parse("#222"))
    }

}
