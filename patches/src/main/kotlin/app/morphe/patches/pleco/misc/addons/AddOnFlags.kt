/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.addons

import app.morphe.patches.readera.shared.MaskedPattern
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MOV_W0_1 = 0x52800020
private const val MOV_W8_1 = 0x52800028
private const val RET = 0xd65f03c0.toInt()
private const val MOVZ_W8 = 0x52800008

private const val OCR_FLAG = 0x5754
private const val DOCUMENT_READER_FLAG = 0x5757
private const val FLASHCARDS_FLAG = 0x5758

internal object AddOnFlags {
    const val LIBRARY = "lib/arm64-v8a/libplecoengine.so"

    private val ownedFlagGetters = listOf(
        "ocrGetter" to OCR_FLAG,
        "documentReaderGetter" to DOCUMENT_READER_FLAG,
        "flashcardsGetter" to FLASHCARDS_FLAG,
    ).map { (name, flag) -> MaskedPattern(name, "${littleEndianHex(MOVZ_W8 or (flag shl 5))} 00 68 68 38 c0 03 5f d6") }

    private val handwritingGetter = MaskedPattern(
        "handwritingGetter",
        "08 14 40 91 09 3d 5d 39 a9 00 00 34 08 41 5d 39 1f 01 00 71 e0 07 9f 1a c0 03 5f d6",
    )
    private const val HANDWRITING_OWNED_LOAD_OFFSET = 12

    fun forceOwnedFlags(library: ByteArray) {
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        ownedFlagGetters.forEach { getter ->
            val start = getter.singleIn(library)
            code.putInt(start, MOV_W0_1)
            code.putInt(start + 4, RET)
        }
        code.putInt(handwritingGetter.singleIn(library) + HANDWRITING_OWNED_LOAD_OFFSET, MOV_W8_1)
    }

    private fun littleEndianHex(word: Int) = (0 until 4).joinToString(" ") { "%02x".format((word ushr (it * 8)) and 0xff) }
}
