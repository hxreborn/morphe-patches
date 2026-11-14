/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.moves

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.candycrushsaga.shared.candyCrushModifiersPatch
import app.morphe.patches.candycrushsaga.shared.candyCrushNativeBuild
import app.morphe.patches.candycrushsaga.shared.candyCrushNativePatch
import app.morphe.patches.candycrushsaga.shared.isCandyCrushSodaSaga
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.replaceStub
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MODIFIERS = "Lapp/hxreborn/extension/candycrushsaga/GameModifiers;"

private const val INITIALIZE_MOVES_OFFSET = 0x32a8d28
private const val ADJUST_MOVES_OFFSET = 0x7889b0
private const val STORE_MOVES_OFFSET = 0x1a95334
private const val DISPLAY_MOVES_OFFSET = 0x27c04a0
private const val ADJUST_DISPLAY_OFFSET = 0x4dcde74
private const val FINISH_DISPLAY_OFFSET = 0x4dcdeb4
private const val DISPLAY_MOVES_CODE = 0x2a0103f3
private const val CALL_ADJUST_DISPLAY = 0x94983675.toInt()
private val ADJUST_DISPLAY_CODE = intArrayOf(0x31001433, 0x1a936033, 0x1400000e)
private val FINISH_DISPLAY_CODE = intArrayOf(0x7100003f, 0x1a81c273, 0xd65f03c0.toInt())
private const val BRANCH_TO_ADJUST_MOVES = 0x17537f22
private val INITIALIZE_MOVES_CODE = intArrayOf(0x29000401, 0x29017c01, 0xb9001001.toInt(), 0xd65f03c0.toInt())
private val ADJUST_MOVES_CODE = intArrayOf(0x31001428, 0x1a886028, 0x7100003f, 0x144c325e)
private val STORE_MOVES_CODE = intArrayOf(0x1a81c101, 0x29000401, 0x14604e7c)

private val addSodaExtraMovesPatch = resourcePatch {
    dependsOn(candyCrushNativePatch)

    execute {
        if (!isCandyCrushSodaSaga(packageMetadata.packageName)) return@execute
        val library = get(candyCrushNativeBuild(packageMetadata.packageName).libraryEntry)
        val bytes = library.readBytes()
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun requireInstructions(offset: Int, expected: IntArray) {
            expected.forEachIndexed { index, instruction ->
                val address = offset + index * Int.SIZE_BYTES
                val observed = words.getInt(address)
                if (observed != instruction) {
                    throw PatchException(
                        "Soda extra moves instructions mismatch at 0x${address.toString(16)}: " +
                            "expected 0x${instruction.toUInt().toString(16)}, got 0x${observed.toUInt().toString(16)}",
                    )
                }
            }
        }
        requireInstructions(INITIALIZE_MOVES_OFFSET, INITIALIZE_MOVES_CODE)
        requireInstructions(DISPLAY_MOVES_OFFSET, intArrayOf(DISPLAY_MOVES_CODE))
        requireInstructions(ADJUST_DISPLAY_OFFSET, IntArray(ADJUST_DISPLAY_CODE.size))
        requireInstructions(FINISH_DISPLAY_OFFSET, IntArray(FINISH_DISPLAY_CODE.size))
        requireInstructions(ADJUST_MOVES_OFFSET, IntArray(ADJUST_MOVES_CODE.size))
        requireInstructions(STORE_MOVES_OFFSET, IntArray(STORE_MOVES_CODE.size))
        fun writeInstructions(offset: Int, instructions: IntArray) {
            instructions.forEachIndexed { index, instruction ->
                words.putInt(offset + index * Int.SIZE_BYTES, instruction)
            }
        }
        writeInstructions(INITIALIZE_MOVES_OFFSET, intArrayOf(BRANCH_TO_ADJUST_MOVES))
        writeInstructions(ADJUST_MOVES_OFFSET, ADJUST_MOVES_CODE)
        writeInstructions(STORE_MOVES_OFFSET, STORE_MOVES_CODE)
        writeInstructions(DISPLAY_MOVES_OFFSET, intArrayOf(CALL_ADJUST_DISPLAY))
        writeInstructions(ADJUST_DISPLAY_OFFSET, ADJUST_DISPLAY_CODE)
        writeInstructions(FINISH_DISPLAY_OFFSET, FINISH_DISPLAY_CODE)
        library.writeBytes(bytes)
    }
}

@Suppress("unused")
val addExtraMovesPatch = bytecodePatch(
    name = "Add extra moves",
    description = "Adds five moves when each level starts. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(candyCrushModifiersPatch, addSodaExtraMovesPatch)

    execute {
        mutableClassDefBy(MODIFIERS).replaceStub("extraMovesEnabled", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
