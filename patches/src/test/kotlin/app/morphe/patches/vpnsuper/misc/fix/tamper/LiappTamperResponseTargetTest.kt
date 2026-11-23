/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.fix.tamper

import app.morphe.patches.shared.misc.AbiTarget
import app.morphe.patches.shared.misc.NativeCheck
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class LiappTamperResponseTargetTest {
    private val fillerByte: Byte = 0x2A

    private fun AbiTarget.siteSpacing() = checks.maxOf { it.pattern.size } * 2

    private fun AbiTarget.givenLibraryWithEveryPattern(): ByteArray {
        val spacing = siteSpacing()
        val library = ByteArray(spacing * (checks.size + 1)) { fillerByte }
        checks.forEachIndexed { index, check -> check.pattern.copyInto(library, spacing * (index + 1)) }
        return library
    }

    private fun givenStockLibrary(variable: String): ByteArray {
        val configured = System.getenv(variable)
        assumeTrue(configured != null, "$variable is not set")
        return File(configured!!).readBytes()
    }

    private fun sitesOf(library: ByteArray, needle: ByteArray) =
        (0..library.size - needle.size).filter { at -> needle.indices.all { library[at + it] == needle[it] } }

    private fun assertReplacedAt(library: ByteArray, check: NativeCheck, site: Int) =
        assertContentEquals(check.replacement, library.copyOfRange(site, site + check.replacement.size), check.name)

    private fun assertReplacesEveryPattern(target: AbiTarget) {
        // given
        val library = target.givenLibraryWithEveryPattern()
        val spacing = target.siteSpacing()

        // when
        target.applyTo(library)

        // then
        target.checks.forEachIndexed { index, check -> assertReplacedAt(library, check, spacing * (index + 1)) }
    }

    private fun assertPatchesStockLibrary(target: AbiTarget, variable: String) {
        // given
        val library = givenStockLibrary(variable)
        val siteByCheck = target.checks.associateWith { check ->
            val sites = sitesOf(library, check.pattern)
            assertEquals(1, sites.size, "${check.name} matched ${sites.size} sites")
            sites.single()
        }

        // when
        target.applyTo(library)

        // then
        siteByCheck.forEach { (check, site) -> assertReplacedAt(library, check, site) }
    }

    @Test
    fun `replaces every arm64 check pattern`() = assertReplacesEveryPattern(LiappTamperResponseTarget.ARM64)

    @Test
    fun `replaces every armeabi-v7a check pattern`() = assertReplacesEveryPattern(LiappTamperResponseTarget.ARM32)

    @Test
    fun `patches the stock arm64 library once per check`() =
        assertPatchesStockLibrary(LiappTamperResponseTarget.ARM64, "VPNSUPER_LIBXXEZVW_ARM64")

    @Test
    fun `patches the stock armeabi-v7a library once per check`() =
        assertPatchesStockLibrary(LiappTamperResponseTarget.ARM32, "VPNSUPER_LIBXXEZVW_ARM32")
}
