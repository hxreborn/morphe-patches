/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

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

internal class AbiTarget(val libraryPath: String, val checks: List<NativeCheck>) {
    fun applyTo(library: ByteArray) = checks.forEach { it.applyTo(library) }
}

private fun ByteArray.regionMatches(at: Int, needle: ByteArray): Boolean {
    if (at < 0 || at + needle.size > size) return false
    for (index in needle.indices) if (this[at + index] != needle[index]) return false
    return true
}

private fun ByteArray.indicesOf(needle: ByteArray): List<Int> =
    if (needle.isEmpty()) emptyList()
    else (0..size - needle.size).filter { regionMatches(it, needle) }
