/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.walp.misc.premium

import app.morphe.patches.shared.replaceMaskedEvery
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ProPreferenceReadTest {
    private val noise: Byte = 0x2A
    private val loadTrueIntoX0 = "c0820091".hexToByteArray()

    private fun givenLibraryWith(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(8192) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun readSiteCalling(branchOffset: Int) =
        ADS_REMOVED_PREFERENCE_READ.copyOf().also {
            it[4] = branchOffset.toByte()
            it[5] = (branchOffset shr 8).toByte()
        }

    private fun patch(library: File) =
        library.replaceMaskedEvery(ADS_REMOVED_PREFERENCE_READ, ADS_REMOVED_PREFERENCE_READ_MASK, 4, loadTrueIntoX0)

    @Test
    fun `replaces the call at every read site whatever its branch target`() {
        // given
        val library = givenLibraryWith(256 to readSiteCalling(0x12), 4096 to readSiteCalling(0x3456))

        // when
        val replaced = patch(library)

        // then
        assertEquals(2, replaced)
        val bytes = library.readBytes()
        listOf(256, 4096).forEach { at ->
            assertContentEquals(ADS_REMOVED_PREFERENCE_READ.copyOfRange(0, 4), bytes.copyOfRange(at, at + 4))
            assertContentEquals(loadTrueIntoX0, bytes.copyOfRange(at + 4, at + 8))
        }
    }

    @Test
    fun `leaves a library without read sites untouched`() {
        // given
        val library = givenLibraryWith()

        // when
        val replaced = patch(library)

        // then
        assertEquals(0, replaced)
        assertTrue(library.readBytes().all { it == noise })
    }
}
