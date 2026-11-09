/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.freedom.misc.upsell

import app.morphe.patches.shared.replaceMasked
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

internal class UpsellSitesTest {
    private val noise: Byte = 0x2A
    private val siteOffset = 0x200
    private val maskedLdurImmediateIndex = 49

    private fun givenLibraryWith(site: ByteArray): File {
        val bytes = ByteArray(4096) { noise }
        site.copyInto(bytes, siteOffset)
        return File.createTempFile("libapp", ".so").apply { deleteOnExit(); writeBytes(bytes) }
    }

    @Test
    fun `branches past the paywall call whatever the ldur immediate`() {
        // given
        val siteWithOtherLdurImmediate = ONBOARDING_PAYWALL_CALL.copyOf().also { it[maskedLdurImmediateIndex] = 0x30 }
        val library = givenLibraryWith(siteWithOtherLdurImmediate)

        // when
        val replaced = library.replaceMasked(
            ONBOARDING_PAYWALL_CALL,
            ONBOARDING_PAYWALL_CALL_MASK,
            mapOf(0 to BRANCH_PAST_ONBOARDING_PAYWALL),
        )

        // then
        assertTrue(replaced)
        val patched = library.readBytes()
        assertContentEquals(BRANCH_PAST_ONBOARDING_PAYWALL, patched.copyOfRange(siteOffset, siteOffset + BRANCH_PAST_ONBOARDING_PAYWALL.size))
        assertContentEquals(
            siteWithOtherLdurImmediate.copyOfRange(BRANCH_PAST_ONBOARDING_PAYWALL.size, siteWithOtherLdurImmediate.size),
            patched.copyOfRange(siteOffset + BRANCH_PAST_ONBOARDING_PAYWALL.size, siteOffset + siteWithOtherLdurImmediate.size),
        )
    }

    @Test
    fun `shortens the onboarding outro to its first step`() {
        // given
        val library = givenLibraryWith(OUTRO_STEP_LIST)

        // when
        val replaced = library.replaceMasked(
            OUTRO_STEP_LIST,
            OUTRO_STEP_LIST_MASK,
            mapOf(OUTRO_LENGTH_OFFSET to OUTRO_LENGTH_ONE_STEP),
        )

        // then
        assertTrue(replaced)
        val lengthAt = siteOffset + OUTRO_LENGTH_OFFSET
        assertContentEquals(OUTRO_LENGTH_ONE_STEP, library.readBytes().copyOfRange(lengthAt, lengthAt + OUTRO_LENGTH_ONE_STEP.size))
    }

    @Test
    fun `loads false in place of the Today free user check`() {
        // given
        val library = givenLibraryWith(TODAY_FREE_USER_CHECK)

        // when
        val replaced = library.replaceMasked(
            TODAY_FREE_USER_CHECK,
            TODAY_FREE_USER_CHECK_MASK,
            mapOf(TODAY_FREE_USER_CALL_OFFSET to LOAD_FALSE),
        )

        // then
        assertTrue(replaced)
        val callAt = siteOffset + TODAY_FREE_USER_CALL_OFFSET
        assertContentEquals(LOAD_FALSE, library.readBytes().copyOfRange(callAt, callAt + LOAD_FALSE.size))
    }
}
