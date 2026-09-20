/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class AtvToolsSignatureCheckTargetTest {
    private val noise: Byte = 0x2A

    private fun libraryWith(pattern: ByteArray, at: Int, size: Int = 8192): ByteArray {
        val library = ByteArray(size) { noise }
        pattern.copyInto(library, at)
        return library
    }

    @Test
    fun `each check replaces its own site and leaves the rest untouched`() {
        AtvToolsSignatureCheckTarget.arm32Checks.forEach { check ->
            val at = 2048
            val library = libraryWith(check.pattern, at)

            check.applyTo(library)

            assertContentEquals(check.replacement, library.copyOfRange(at, at + check.replacement.size), check.name)
            assertTrue(library.copyOfRange(0, at).all { it == noise }, "${check.name}: bytes before the site changed")
            assertTrue(
                library.copyOfRange(at + check.replacement.size, library.size).all { it == noise },
                "${check.name}: bytes after the site changed",
            )
        }
    }

    @Test
    fun `a missing site fails loudly and names the check`() {
        val check = AtvToolsSignatureCheckTarget.arm32Checks.first()
        val message = assertFailsWith<PatchException> { check.applyTo(ByteArray(8192) { noise }) }.message
        assertTrue(message!!.contains(check.name) && message.contains("found 0"), message)
    }

    @Test
    fun `an ambiguous site fails loudly with the match count`() {
        val check = AtvToolsSignatureCheckTarget.arm32Checks.first()
        val library = ByteArray(8192) { noise }
        check.pattern.copyInto(library, 256)
        check.pattern.copyInto(library, 4096)

        val message = assertFailsWith<PatchException> { check.applyTo(library) }.message
        assertTrue(message!!.contains(check.name) && message.contains("found 2"), message)
    }

    @Test
    fun `an already patched library is rejected rather than silently skipped`() {
        val check = AtvToolsSignatureCheckTarget.arm32Checks.first()
        val message = assertFailsWith<PatchException> { check.applyTo(libraryWith(check.replacement, 512)) }.message
        assertTrue(message!!.contains(check.name) && message.contains("found 0"), message)
    }

    @Test
    fun `a library that is not the expected atvTools native library is rejected`() {
        val message = assertFailsWith<PatchException> {
            AtvToolsSignatureCheckTarget.applyArm32(ByteArray(16384) { (it * 31 + 7).toByte() })
        }.message
        assertTrue(message!!.contains("found 0"), message)
    }

    @Test
    fun `a check whose pattern and replacement differ in length is rejected at definition`() {
        val message = assertFailsWith<PatchException> { NativeCheck("mismatched", "00 11 22", "00 11") }.message
        assertTrue(message!!.contains("mismatched"), message)
    }

    @Test
    fun `applyArm32 replaces every check pattern on one library`() {
        val library = ByteArray(16384) { noise }
        val sites = AtvToolsSignatureCheckTarget.arm32Checks.mapIndexed { index, check ->
            val at = 1024 + index * 4096
            check.pattern.copyInto(library, at)
            check to at
        }

        AtvToolsSignatureCheckTarget.applyArm32(library)

        sites.forEach { (check, at) ->
            assertContentEquals(check.replacement, library.copyOfRange(at, at + check.replacement.size), check.name)
        }
    }

    @Test
    fun `each check matches exactly one site and replaces it across the stock samples`() {
        val configured = System.getenv("ATVTOOLS_STOCK_XAPK")
        assumeTrue(configured != null, "ATVTOOLS_STOCK_XAPK is not set")

        configured!!.split(File.pathSeparatorChar).filter { it.isNotBlank() }.forEach { path ->
            val xapk = File(path)
            assertTrue(xapk.isFile, "configured stock XAPK does not exist: $xapk")
            val library = arm32LibraryOf(xapk)

            val sites = AtvToolsSignatureCheckTarget.arm32Checks.map { check ->
                val hits = indicesOf(library, check.pattern)
                assertEquals(1, hits.size, "${xapk.name}: ${check.name} matched ${hits.size} sites")
                check to hits.single()
            }

            AtvToolsSignatureCheckTarget.applyArm32(library)

            sites.forEach { (check, at) ->
                assertTrue(
                    regionMatches(library, at, check.replacement),
                    "${xapk.name}: ${check.name} was not replaced at $at",
                )
            }
        }
    }

    private fun arm32LibraryOf(xapk: File): ByteArray {
        ZipFile(xapk).use { outer ->
            outer.entries().asSequence().filter { it.name.endsWith(".apk") }.forEach { split ->
                ZipInputStream(outer.getInputStream(split)).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        if (entry.name == AtvToolsSignatureCheckTarget.ARM32) return zip.readBytes()
                    }
                }
            }
        }
        throw AssertionError("${xapk.name} has no ${AtvToolsSignatureCheckTarget.ARM32}")
    }

    private fun regionMatches(haystack: ByteArray, at: Int, needle: ByteArray): Boolean {
        if (at + needle.size > haystack.size) return false
        for (index in needle.indices) if (haystack[at + index] != needle[index]) return false
        return true
    }

    private fun indicesOf(haystack: ByteArray, needle: ByteArray): List<Int> {
        val sites = ArrayList<Int>()
        var start = 0
        val lastStart = haystack.size - needle.size
        while (start <= lastStart) {
            if (regionMatches(haystack, start, needle)) sites += start
            start++
        }
        return sites
    }
}
