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
        val site = sites.singleOrNull()
            ?: throw PatchException("$name: expected 1 match, found ${sites.size}")

        replacement.copyInto(library, site)
        if (!library.regionMatches(site, replacement)) {
            throw PatchException("$name: replacement not present at $site after write")
        }
    }
}

internal class AbiTarget(val library: String, val checks: List<NativeCheck>) {
    fun applyTo(bytes: ByteArray) = checks.forEach { it.applyTo(bytes) }
}

internal object AtvToolsSignatureCheckTarget {
    val ARM32 = AbiTarget(
        "lib/armeabi-v7a/liba.so",
        listOf(
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
        ),
    )

    val ARM64 = AbiTarget(
        "lib/arm64-v8a/liba.so",
        listOf(
            NativeCheck(
                "certificateComparisonFirst",
                "bf 02 00 72 80 16 9c 9a 58 89 11 94 69 32 40 91",
                "bf 02 00 72 80 16 9c 9a 00 00 80 52 69 32 40 91",
            ),
            NativeCheck(
                "certificateComparisonSecond",
                "21 28 00 90 21 40 35 91 fa 03 00 aa 8e 74 11 94 a8 02 40 f9",
                "21 28 00 90 21 40 35 91 fa 03 00 aa 00 00 80 52 a8 02 40 f9",
            ),
            NativeCheck(
                "runtimeIntegrityCheck",
                "fd 7b bb a9 fc 0b 00 f9 f8 5f 02 a9 " +
                    "f6 57 03 a9 f4 4f 04 a9 fd 03 00 91 ff c3 09 d1",
                "c0 03 5f d6 fc 0b 00 f9 f8 5f 02 a9 " +
                    "f6 57 03 a9 f4 4f 04 a9 fd 03 00 91 ff c3 09 d1",
            ),
        ),
    )

    val targets = listOf(ARM32, ARM64)
}

private fun ByteArray.regionMatches(at: Int, needle: ByteArray): Boolean {
    if (at < 0 || at + needle.size > size) return false
    for (index in needle.indices) if (this[at + index] != needle[index]) return false
    return true
}

private fun ByteArray.indicesOf(needle: ByteArray): List<Int> =
    if (needle.isEmpty()) emptyList()
    else (0..size - needle.size).filter { regionMatches(it, needle) }
