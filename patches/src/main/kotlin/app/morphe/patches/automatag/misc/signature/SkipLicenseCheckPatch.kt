/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.automatag.misc.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.netanalyzer.shared.Arm64
import app.morphe.patches.netanalyzer.shared.Arm64ElfReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val LIBRARY = "lib/arm64-v8a/libtaglib.so"
private const val IS_ORIGINAL_SYMBOL = "Java_helpers_Auth_isOriginal"
private const val IS_ORIGINAL_SCAN_INSTRUCTIONS = 8
private const val MOV_W0_1 = 0x52800020
private const val RET = 0xd65f03c0.toInt()

internal val skipLicenseCheckPatch = rawResourcePatch {
    execute {
        val library = get(LIBRARY, true)
        if (!library.exists()) throw PatchException("$LIBRARY is missing")

        val bytes = library.readBytes()
        val elf = Arm64ElfReader(bytes)
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val isOriginal = elf.fileOffset(elf.symbol(IS_ORIGINAL_SYMBOL))
        val checkCall = (0 until IS_ORIGINAL_SCAN_INSTRUCTIONS)
            .map { isOriginal + it * Arm64.INSTRUCTION_BYTES }
            .firstOrNull { Arm64.isBranchLink(words.getInt(it)) }
            ?: throw PatchException("$IS_ORIGINAL_SYMBOL: no bl in its first $IS_ORIGINAL_SCAN_INSTRUCTIONS instructions")
        val signatureCheck = Arm64.branchLinkTarget(words.getInt(checkCall), checkCall)

        words.putInt(signatureCheck, MOV_W0_1)
        words.putInt(signatureCheck + Arm64.INSTRUCTION_BYTES, RET)
        library.writeBytes(bytes)
    }
}
