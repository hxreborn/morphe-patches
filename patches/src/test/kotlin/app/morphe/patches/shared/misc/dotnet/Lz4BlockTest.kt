/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal const val ABC_REPEATED_THEN_X = "abcabcabcabcx"
internal val ABC_REPEATED_THEN_X_LZ4 =
    byteArrayOf(0x35, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(), 0x03, 0x00, 0x10, 'x'.code.toByte())

class Lz4BlockTest {
    @Test
    fun decompressOverlappingMatch() {
        val block = ABC_REPEATED_THEN_X_LZ4

        val text = String(decompressLz4Block(block, 0, block.size, ABC_REPEATED_THEN_X.length))

        assertEquals(ABC_REPEATED_THEN_X, text)
    }

    @Test
    fun rejectMatchBeforeOutputStart() {
        val block = byteArrayOf(0x15, 'a'.code.toByte(), 0x02, 0x00)

        assertFailsWith<PatchException> { decompressLz4Block(block, 0, block.size, 10) }
    }
}
