/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.freedom.misc.lockedmode

import app.morphe.patches.shared.replaceMasked
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class LockedModeGateTest {
    private val noise: Byte = 0x2A
    private val checkAt = 0x100
    private val resetAt = 0x800
    private val resetBranchOffset = 24
    private val premiumCheckPoolOffsetIndex = 34

    private fun givenLibraryWith(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(4096) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun withPoolOffset(site: ByteArray, at: Int) = site.copyOf().also { it[at] = 0x55 }

    private fun patchPremiumCheck(library: File) =
        library.replaceMasked(LOCKED_MODE_PREMIUM_CHECK, LOCKED_MODE_PREMIUM_CHECK_MASK, mapOf(0 to RETURN_TRUE))

    private fun patchReset(library: File) =
        library.replaceMasked(
            LOCKED_MODE_RESET,
            LOCKED_MODE_RESET_MASK,
            mapOf(resetBranchOffset to SKIP_LOCKED_MODE_RESET),
        )

    @Test
    fun `returns true from the premium check whatever its pool offset`() {
        // given
        val library = givenLibraryWith(checkAt to withPoolOffset(LOCKED_MODE_PREMIUM_CHECK, premiumCheckPoolOffsetIndex))

        // when
        val replaced = patchPremiumCheck(library)

        // then
        assertTrue(replaced)
        assertContentEquals(RETURN_TRUE, library.readBytes().copyOfRange(checkAt, checkAt + RETURN_TRUE.size))
    }

    @Test
    fun `branches past the Locked Mode reset`() {
        // given
        val library = givenLibraryWith(resetAt to LOCKED_MODE_RESET)

        // when
        val replaced = patchReset(library)

        // then
        assertTrue(replaced)
        val branchAt = resetAt + resetBranchOffset
        assertContentEquals(SKIP_LOCKED_MODE_RESET, library.readBytes().copyOfRange(branchAt, branchAt + SKIP_LOCKED_MODE_RESET.size))
    }

    @Test
    fun `replaces the tap compare with a self compare and keeps the bytes after it`() {
        // given
        val tapAt = 0x400
        val library = givenLibraryWith(tapAt to LOCKED_MODE_TAP_CHECK)

        // when
        val replaced = library.replaceMasked(
            LOCKED_MODE_TAP_CHECK,
            LOCKED_MODE_TAP_CHECK_MASK,
            mapOf(TAP_COMPARE_OFFSET to COMPARE_W1_WITH_ITSELF),
        )

        // then
        assertTrue(replaced)
        val compareAt = tapAt + TAP_COMPARE_OFFSET
        val branchAt = compareAt + COMPARE_W1_WITH_ITSELF.size
        val patched = library.readBytes()
        assertContentEquals(COMPARE_W1_WITH_ITSELF, patched.copyOfRange(compareAt, branchAt))
        assertContentEquals(
            LOCKED_MODE_TAP_CHECK.copyOfRange(branchAt - tapAt, LOCKED_MODE_TAP_CHECK.size),
            patched.copyOfRange(branchAt, tapAt + LOCKED_MODE_TAP_CHECK.size),
        )
    }

    @Test
    fun `leaves a library without the reset untouched`() {
        // given
        val library = givenLibraryWith()

        // when
        val replaced = patchReset(library)

        // then
        assertFalse(replaced)
        assertTrue(library.readBytes().all { it == noise })
    }
}
