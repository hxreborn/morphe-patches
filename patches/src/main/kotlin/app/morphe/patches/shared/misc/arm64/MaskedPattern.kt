/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.arm64

import app.morphe.patcher.patch.PatchException

internal const val ANY_WORD = "?? ?? ?? ??"

internal class MaskedPattern(val name: String, hex: String) {
    private val bytes = hex.trim().split(Regex("\\s+")).map { if (it == "??") null else it.toInt(16).toByte() }

    val size get() = bytes.size

    fun indicesIn(library: ByteArray): List<Int> = (0..library.size - bytes.size).filter { start ->
        bytes.indices.all { bytes[it] == null || library[start + it] == bytes[it] }
    }

    fun singleIn(library: ByteArray): Int = indicesIn(library).singleOrNull()
        ?: throw PatchException("$name: expected 1 match, found ${indicesIn(library).size}")
}
