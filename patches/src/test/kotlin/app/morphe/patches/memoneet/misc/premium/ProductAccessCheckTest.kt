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

internal class ProductAccessCheckTest {
    private val noise: Byte = 0x2A

    private fun libraryOf(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(8192) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun patch(library: File) =
        library.replaceMasked(PRODUCT_ACCESS_CHECK_PROLOGUE, PRODUCT_ACCESS_CHECK_PROLOGUE_MASK, mapOf(0 to RETURN_TRUE))

    @Test
    fun `the mask covers the pattern and the replacement fits inside it`() {
        assertEquals(PRODUCT_ACCESS_CHECK_PROLOGUE.size, PRODUCT_ACCESS_CHECK_PROLOGUE_MASK.size)
        assertTrue(RETURN_TRUE.size <= PRODUCT_ACCESS_CHECK_PROLOGUE.size)
    }

    @Test
    fun `the check prologue is replaced with the return-true stub and nothing else changes`() {
        val at = 2048
        val library = libraryOf(at to PRODUCT_ACCESS_CHECK_PROLOGUE)

        assertTrue(patch(library))

        val bytes = library.readBytes()
        assertContentEquals(RETURN_TRUE, bytes.copyOfRange(at, at + RETURN_TRUE.size))
        assertContentEquals(
            PRODUCT_ACCESS_CHECK_PROLOGUE.copyOfRange(RETURN_TRUE.size, PRODUCT_ACCESS_CHECK_PROLOGUE.size),
            bytes.copyOfRange(at + RETURN_TRUE.size, at + PRODUCT_ACCESS_CHECK_PROLOGUE.size),
        )
        assertTrue(bytes.copyOfRange(0, at).all { it == noise })
        assertTrue(bytes.copyOfRange(at + PRODUCT_ACCESS_CHECK_PROLOGUE.size, bytes.size).all { it == noise })
    }

    @Test
    fun `the bits the mask ignores may differ between builds`() {
        val rebuilt = ByteArray(PRODUCT_ACCESS_CHECK_PROLOGUE.size) { index ->
            (PRODUCT_ACCESS_CHECK_PROLOGUE[index].toInt() xor PRODUCT_ACCESS_CHECK_PROLOGUE_MASK[index].toInt().inv()).toByte()
        }

        assertTrue(patch(libraryOf(2048 to rebuilt)))
    }

    @Test
    fun `a changed instruction the mask compares is not a match`() {
        val changed = PRODUCT_ACCESS_CHECK_PROLOGUE.copyOf()
        changed[0] = (changed[0].toInt() xor 0xFF).toByte()

        assertFalse(patch(libraryOf(2048 to changed)))
    }

    @Test
    fun `a library without the check reports no match`() {
        assertFalse(patch(libraryOf()))
    }

    @Test
    fun `a prologue matching twice throws`() {
        val library = libraryOf(256 to PRODUCT_ACCESS_CHECK_PROLOGUE, 4096 to PRODUCT_ACCESS_CHECK_PROLOGUE)

        assertFailsWith<PatchException> { patch(library) }
    }
}
