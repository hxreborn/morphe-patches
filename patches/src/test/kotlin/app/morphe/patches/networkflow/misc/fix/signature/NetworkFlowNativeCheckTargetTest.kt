/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.fix.signature

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class NetworkFlowNativeCheckTargetTest {
    private val target = NetworkFlowNativeCheckTarget.ARM64
    private val checks = target.checks
    private val noise: Byte = 0x2A
    private val siteSpacing = checks.maxOf { it.pattern.size } * 2

    private fun siteOf(index: Int) = siteSpacing * (index + 1)

    private fun givenLibraryWithEveryPattern(): ByteArray {
        val library = ByteArray(siteSpacing * (checks.size + 1)) { noise }
        checks.forEachIndexed { index, check -> check.pattern.copyInto(library, siteOf(index)) }
        return library
    }

    private fun givenStockLibrary(): ByteArray {
        val configured = System.getenv("NETWORKFLOW_LIBENGINE")
        assumeTrue(configured != null, "NETWORKFLOW_LIBENGINE is not set")
        return File(configured!!).readBytes()
    }

    private fun sitesOf(library: ByteArray, needle: ByteArray) =
        (0..library.size - needle.size).filter { at -> needle.indices.all { library[at + it] == needle[it] } }

    @Test
    fun `replaces every check pattern in one library`() {
        // given
        val library = givenLibraryWithEveryPattern()

        // when
        target.applyTo(library)

        // then
        checks.forEachIndexed { index, check ->
            val site = siteOf(index)
            assertContentEquals(check.replacement, library.copyOfRange(site, site + check.replacement.size), check.name)
        }
    }

    @Test
    fun `matches each check pattern exactly once in the stock library`() {
        // given
        val library = givenStockLibrary()

        // when
        val sitesByCheck = checks.associateWith { sitesOf(library, it.pattern) }

        // then
        sitesByCheck.forEach { (check, sites) ->
            assertEquals(1, sites.size, "${check.name} matched ${sites.size} sites")
        }
    }

    @Test
    fun `writes each check replacement over its pattern in the stock library`() {
        // given
        val library = givenStockLibrary()
        val siteByCheck = checks.associateWith { sitesOf(library, it.pattern).single() }

        // when
        target.applyTo(library)

        // then
        siteByCheck.forEach { (check, site) ->
            assertContentEquals(check.replacement, library.copyOfRange(site, site + check.replacement.size), check.name)
        }
    }
}
