/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val LIBRARY = "lib/arm64-v8a/libnative.so"
private const val SIGNATURE_MISMATCH_BIT = 0
private const val KILL_DELAY_BASE_MILLISECONDS = 26_000
private const val KILL_DELAY_SEARCH_INSTRUCTIONS = 32
private const val CALLS_AFTER_GATE = 2

internal val disableNativeTamperCheckPatch = rawResourcePatch {
    execute {
        val library = get(LIBRARY, true)
        if (!library.exists()) throw PatchException("$LIBRARY is missing")

        val bytes = library.readBytes()
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val step = Arm64.INSTRUCTION_BYTES

        fun buildsKillDelay(function: Int) = (0 until KILL_DELAY_SEARCH_INSTRUCTIONS).any { index ->
            val offset = function + index * step
            offset + step <= bytes.size &&
                Arm64.isMoveWideZeroW(words.getInt(offset), KILL_DELAY_BASE_MILLISECONDS)
        }

        val gates = (0..bytes.size - (CALLS_AFTER_GATE + 1) * step step step).filter { offset ->
            Arm64.isTestBitZeroBranch(words.getInt(offset), SIGNATURE_MISMATCH_BIT) &&
                (1..CALLS_AFTER_GATE).all { Arm64.isBranchLink(words.getInt(offset + it * step)) } &&
                buildsKillDelay(Arm64.branchLinkTarget(words.getInt(offset + step), offset + step))
        }
        val gate = gates.singleOrNull()
            ?: throw PatchException("Expected one signature gate in $LIBRARY, found ${gates.size}")

        words.putInt(gate, Arm64.branch(Arm64.testBranchOffsetWords(words.getInt(gate))))
        library.writeBytes(bytes)
    }
}
