/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.walp.misc.premium

import app.morphe.patches.shared.replaceMasked
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

internal class PurchaseStateCopyWithTest {
    private val noise: Byte = 0x2A
    private val loadTrueIntoX1 = "c1820091".hexToByteArray()
    private val otherFrameSizeSubtract = "ef0104d1".hexToByteArray()
    private val frameSizeSubtractFromEnd = 16
    private val patchedInstructionBytes = 4..8
    private val purchaseStateSiteAt = 256
    private val otherClassSiteAt = 4096

    private fun givenLibraryWith(vararg sites: Pair<Int, ByteArray>): File {
        val bytes = ByteArray(8192) { noise }
        sites.forEach { (at, site) -> site.copyInto(bytes, at) }
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    private fun siteFollowedByOtherFunction() = PURCHASE_STATE_COPY_WITH_TAIL.copyOf().also {
        otherFrameSizeSubtract.copyInto(it, it.size - frameSizeSubtractFromEnd)
    }

    private fun patch(library: File) = library.replaceMasked(
        PURCHASE_STATE_COPY_WITH_TAIL,
        PURCHASE_STATE_COPY_WITH_TAIL_MASK,
        mapOf(patchedInstructionBytes.first to loadTrueIntoX1),
    )

    private fun File.patchedInstructionAt(site: Int) =
        readBytes().copyOfRange(site + patchedInstructionBytes.first, site + patchedInstructionBytes.last)

    @Test
    fun `patches only the copy whose next function matches`() {
        // given
        val otherClassSite = siteFollowedByOtherFunction()
        val library = givenLibraryWith(
            purchaseStateSiteAt to PURCHASE_STATE_COPY_WITH_TAIL,
            otherClassSiteAt to otherClassSite,
        )

        // when
        val patched = patch(library)

        // then
        assertTrue(patched)
        assertContentEquals(loadTrueIntoX1, library.patchedInstructionAt(purchaseStateSiteAt))
        assertContentEquals(
            otherClassSite.copyOfRange(patchedInstructionBytes.first, patchedInstructionBytes.last),
            library.patchedInstructionAt(otherClassSiteAt),
        )
    }
}
