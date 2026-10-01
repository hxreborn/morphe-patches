/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.memoneet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.replaceMasked
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ProductAccessGateTest {
    private val noise: Byte = 0x2A

    private fun libraryOf(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(8192) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun patch(library: File) =
        library.replaceMasked(PRODUCT_ACCESS_GATE, PRODUCT_ACCESS_GATE_MASK, mapOf(0 to RETURN_TRUE))

    @Test
    fun `the mask covers the pattern and the replacement fits inside it`() {
        assertEquals(PRODUCT_ACCESS_GATE.size, PRODUCT_ACCESS_GATE_MASK.size)
        assertTrue(RETURN_TRUE.size <= PRODUCT_ACCESS_GATE.size)
    }

    @Test
    fun `the gate returns true and the rest of the library is untouched`() {
        val at = 2048
        val library = libraryOf(at to PRODUCT_ACCESS_GATE)

        assertTrue(patch(library))

        val bytes = library.readBytes()
        assertContentEquals(RETURN_TRUE, bytes.copyOfRange(at, at + RETURN_TRUE.size))
        assertContentEquals(
            PRODUCT_ACCESS_GATE.copyOfRange(RETURN_TRUE.size, PRODUCT_ACCESS_GATE.size),
            bytes.copyOfRange(at + RETURN_TRUE.size, at + PRODUCT_ACCESS_GATE.size),
        )
        assertTrue(bytes.copyOfRange(0, at).all { it == noise })
        assertTrue(bytes.copyOfRange(at + PRODUCT_ACCESS_GATE.size, bytes.size).all { it == noise })
    }

    @Test
    fun `the bits the mask ignores may differ between builds`() {
        val rebuilt = ByteArray(PRODUCT_ACCESS_GATE.size) { index ->
            (PRODUCT_ACCESS_GATE[index].toInt() xor PRODUCT_ACCESS_GATE_MASK[index].toInt().inv()).toByte()
        }

        assertTrue(patch(libraryOf(2048 to rebuilt)))
    }

    @Test
    fun `a changed instruction the mask compares is not a match`() {
        val changed = PRODUCT_ACCESS_GATE.copyOf()
        changed[0] = (changed[0].toInt() xor 0xFF).toByte()

        assertFalse(patch(libraryOf(2048 to changed)))
    }

    @Test
    fun `an unmatched gate reports no match instead of writing`() {
        assertFalse(patch(libraryOf()))
    }

    @Test
    fun `an ambiguous gate fails loudly`() {
        val library = libraryOf(256 to PRODUCT_ACCESS_GATE, 4096 to PRODUCT_ACCESS_GATE)

        assertFailsWith<PatchException> { patch(library) }
    }
}
