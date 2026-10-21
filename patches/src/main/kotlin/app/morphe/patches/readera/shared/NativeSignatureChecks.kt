/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ANY_WORD = "?? ?? ?? ??"
private const val NOP = 0xd503201f.toInt()
private const val RET = 0xd65f03c0.toInt()
private const val MOV_W9_1 = 0x52800029

internal class MaskedPattern(val name: String, hex: String) {
    private val bytes = hex.trim().split(Regex("\\s+")).map { if (it == "??") null else it.toInt(16).toByte() }

    val size get() = bytes.size

    fun indicesIn(library: ByteArray): List<Int> = (0..library.size - bytes.size).filter { start ->
        bytes.indices.all { bytes[it] == null || library[start + it] == bytes[it] }
    }

    fun singleIn(library: ByteArray): Int = indicesIn(library).singleOrNull()
        ?: throw PatchException("$name: expected 1 match, found ${indicesIn(library).size}")
}

internal object NativeSignatureChecks {
    const val LIBRARY = "lib/arm64-v8a/libreadera.so"

    private val verdictSummary = MaskedPattern(
        "verdictSummary",
        "ff 43 00 d1 " + "$ANY_WORD 08 ?? ?? 91 08 01 40 39 ?? ?? 00 36 ".repeat(4) +
            "$ANY_WORD 08 ?? ?? 91 08 01 40 39",
    )

    private val tamperFlagCheckPrologue = "fa 67 bb a9 f8 5f 01 a9 f6 57 02 a9 f4 4f 03 a9 fd 7b 04 a9 fd 03 01 91 " +
        "$ANY_WORD 08 ?? ?? 39 f3 03 02 aa f4 03 01 aa"

    private val checks = listOf(
        MaskedPattern("jniStringCheck", "$tamperFlagCheckPrologue 28 01 00 34") to 0,
        MaskedPattern(
            "jniHashCheck",
            "ff 83 02 d1 fc 6f 04 a9 fa 67 05 a9 f8 5f 06 a9 f6 57 07 a9 f4 4f 08 a9 fd 7b 09 a9 fd 43 02 91 " +
                "$ANY_WORD 08 ?? ?? 39",
        ) to 2,
        MaskedPattern(
            "signingBlockCheck",
            "fc 6f ba a9 fa 67 01 a9 f8 5f 02 a9 f6 57 03 a9 f4 4f 04 a9 fd 7b 05 a9 fd 43 01 91 " +
                "ff c3 0a d1 53 d0 3b d5 68 16 40 f9 fa 03 01 aa fc 03 00 aa",
        ) to 3,
        MaskedPattern("apkDigestCheck", "$tamperFlagCheckPrologue f5 03 00 aa") to 4,
    )

    private val inlineStringCheckBranch = MaskedPattern("inlineStringCheckBranch", "e8 0f 40 b9 88 00 00 36 $ANY_WORD ?? ?? 1d 39")

    fun applyTo(library: ByteArray) {
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        val summary = verdictSummary.singleIn(library)
        val flags = (0 until 5).map { index ->
            val adrpOffset = summary + 4 + index * 16
            pageOf(adrpOffset, code.getInt(adrpOffset)) + ((code.getInt(adrpOffset + 4) ushr 10) and 0xfff)
        }

        checks.forEach { (pattern, flagIndex) ->
            val function = pattern.singleIn(library)
            val flag = flags[flagIndex]
            if (!storesTo(code, function, flag)) {
                throw PatchException("${pattern.name}: no store to its verdict flag at ${flag.toString(16)}")
            }
            code.putInt(function, adrp(function, flag, 8))
            code.putInt(function + 4, MOV_W9_1)
            code.putInt(function + 8, 0x39000000 or ((flag and 0xfff).toInt() shl 10) or (8 shl 5) or 9)
            code.putInt(function + 12, RET)
        }

        val branches = inlineStringCheckBranch.indicesIn(library)
        if (branches.size != 2) throw PatchException("inlineStringCheckBranch: expected 2 matches, found ${branches.size}")
        branches.forEach { code.putInt(it + 4, NOP) }
    }

    private fun pageOf(pc: Int, adrp: Int): Long {
        val immediate = (((adrp ushr 5) and 0x7ffff) shl 2) or ((adrp ushr 29) and 3)
        val signed = (immediate shl 11) shr 11
        return (pc.toLong() and 0xfff.inv().toLong()) + (signed.toLong() shl 12)
    }

    private fun adrp(pc: Int, target: Long, register: Int): Int {
        val pages = ((target and 0xfff.inv().toLong()) - (pc.toLong() and 0xfff.inv().toLong())) shr 12
        return 0x90000000.toInt() or ((pages.toInt() and 3) shl 29) or (((pages.toInt() shr 2) and 0x7ffff) shl 5) or register
    }

    private fun storesTo(code: ByteBuffer, function: Int, flag: Long): Boolean =
        (function until minOf(function + 0x1400, code.limit() - 16) step 4).any { offset ->
            val word = code.getInt(offset)
            (word and 0x9f000000.toInt()) == 0x90000000.toInt() && pageOf(offset, word) == (flag and 0xfff.inv().toLong()) &&
                (1..3).any { step ->
                    val store = code.getInt(offset + step * 4)
                    (store and 0xffc00000.toInt()) == 0x39000000 && ((store ushr 10) and 0xfff).toLong() == (flag and 0xfff)
                }
        }
}
