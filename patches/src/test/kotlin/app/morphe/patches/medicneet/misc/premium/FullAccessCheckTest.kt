/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medicneet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.replaceMasked
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FullAccessCheckTest {
    private val noise: Byte = 0x2A

    private fun libraryOf(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(8192) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun patch(check: DartCheck, library: File) =
        library.replaceMasked(check.pattern, check.mask, mapOf(check.replacementOffset to check.replacement))

    @Test
    fun `keeps every replacement inside its masked pattern`() {
        DART_CHECKS.forEach { check ->
            assertEquals(check.pattern.size, check.mask.size, check.name)
            assertTrue(check.replacementOffset + check.replacement.size <= check.pattern.size, check.name)
        }
    }

    @Test
    fun `replaces only the bytes at the replacement offset`() {
        DART_CHECKS.forEach { check ->
            val at = 2048
            val library = libraryOf(at to check.pattern)

            assertTrue(patch(check, library), check.name)

            val bytes = library.readBytes()
            val expected = check.pattern.copyOf()
            check.replacement.copyInto(expected, check.replacementOffset)
            assertContentEquals(expected, bytes.copyOfRange(at, at + check.pattern.size), check.name)
            assertTrue(bytes.copyOfRange(0, at).all { it == noise }, check.name)
            assertTrue(bytes.copyOfRange(at + check.pattern.size, bytes.size).all { it == noise }, check.name)
        }
    }

    @Test
    fun `ignores masked bits that differ between builds`() {
        DART_CHECKS.forEach { check ->
            val rebuilt = ByteArray(check.pattern.size) { index ->
                (check.pattern[index].toInt() xor check.mask[index].toInt().inv()).toByte()
            }

            assertTrue(patch(check, libraryOf(2048 to rebuilt)), check.name)
        }
    }

    @Test
    fun `rejects a change in a compared instruction`() {
        DART_CHECKS.forEach { check ->
            val changed = check.pattern.copyOf()
            changed[0] = (changed[0].toInt() xor 0xFF).toByte()

            assertFalse(patch(check, libraryOf(2048 to changed)), check.name)
        }
    }

    @Test
    fun `reports no match in a library without the check`() {
        DART_CHECKS.forEach { check -> assertFalse(patch(check, libraryOf()), check.name) }
    }

    @Test
    fun `throws when a pattern matches twice`() {
        DART_CHECKS.forEach { check ->
            val library = libraryOf(256 to check.pattern, 4096 to check.pattern)

            assertFailsWith<PatchException>(check.name) { patch(check, library) }
        }
    }
}
