/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val LIBRARY = "lib/arm64-v8a/libClipStudioPaint.so"
internal const val WORD_SIZE = 4

internal class PatchSite(val pattern: IntArray, val rewrite: (ByteBuffer, Int) -> Unit)

private fun ByteBuffer.matchesAt(offset: Int, words: IntArray) =
    words.indices.all { getInt(offset + it * WORD_SIZE) == words[it] }

private fun ByteBuffer.findPatchSites(patchSites: List<PatchSite>): List<Int> {
    val firstWords = patchSites.map { it.pattern[0] }.toIntArray()
    val found = List(patchSites.size) { mutableListOf<Int>() }
    val longestPatternWords = patchSites.maxOf { it.pattern.size }
    for (offset in 0..limit() - longestPatternWords * WORD_SIZE step WORD_SIZE) {
        val word = getInt(offset)
        for (index in firstWords.indices) {
            if (word == firstWords[index] && matchesAt(offset, patchSites[index].pattern)) found[index] += offset
        }
    }
    found.forEachIndexed { index, offsets ->
        if (offsets.size != 1) {
            throw PatchException("Expected one match for site $index in $LIBRARY, found ${offsets.size}")
        }
    }
    return found.map { it.single() }
}

internal fun ResourcePatchContext.rewriteNativeLibrary(patchSites: List<PatchSite>) {
    val library = get(LIBRARY, true)
    if (!library.exists()) throw PatchException("$LIBRARY not found")

    RandomAccessFile(library, "rw").use { file ->
        val mapped = file.channel.map(FileChannel.MapMode.READ_WRITE, 0, file.length())
        val buffer = mapped.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        buffer.findPatchSites(patchSites).forEachIndexed { index, start -> patchSites[index].rewrite(buffer, start) }
        mapped.force()
    }
}
