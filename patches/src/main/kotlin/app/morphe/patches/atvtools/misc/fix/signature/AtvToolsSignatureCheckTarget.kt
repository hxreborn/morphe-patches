/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.util.byteArrayOf

internal class NativeCheck(val name: String, patternHex: String, replacementHex: String) {
    val pattern = byteArrayOf(patternHex)
    val replacement = byteArrayOf(replacementHex)

    init {
        if (pattern.size != replacement.size) {
            throw PatchException(
                "$name: pattern is ${pattern.size} bytes but replacement is ${replacement.size}",
            )
        }
    }

    fun applyTo(library: ByteArray) {
        val sites = library.indicesOf(pattern)
        if (sites.size != 1) {
            throw PatchException("$name: expected 1 ARM32 match, found ${sites.size}")
        }

        val site = sites.single()
        replacement.copyInto(library, site)
        if (!library.regionMatches(site, replacement)) {
            throw PatchException("$name: replacement not present at $site after write")
        }
    }
}

internal object AtvToolsSignatureCheckTarget {
    const val ARM32 = "lib/armeabi-v7a/liba.so"

    val arm32Checks = listOf(
        NativeCheck(
            "certificateCheck",
            "80 b5 f7 f2 c4 ed 80 bd",
            "00 20 70 47 00 bf 00 bf",
        ),
        NativeCheck(
            "loadTimeDexIntegrityCheck",
            "0b f0 94 fe",
            "af f3 00 80",
        ),
        NativeCheck(
            "runtimeIntegrityCheck",
            "f0 b5 03 af 2d e9 00 0f e3 b0",
            "70 47 03 af 2d e9 00 0f e3 b0",
        ),
    )

    fun applyArm32(library: ByteArray) = arm32Checks.forEach { it.applyTo(library) }
}

private fun ByteArray.regionMatches(at: Int, needle: ByteArray): Boolean {
    if (at < 0 || at + needle.size > size) return false
    for (index in needle.indices) if (this[at + index] != needle[index]) return false
    return true
}

private fun ByteArray.indicesOf(needle: ByteArray): List<Int> =
    if (needle.isEmpty()) emptyList()
    else (0..size - needle.size).filter { regionMatches(it, needle) }
