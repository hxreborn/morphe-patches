/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.netanalyzer.shared.Arm64
import app.morphe.patches.netanalyzer.shared.Arm64ElfReader
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val LIBRARY = "lib/arm64-v8a/lib_musicolet.so"
private const val LOUDNESS_SYMBOL = "Java_in_krosbits_nativex_FDTS_tg_1clr"
private const val LOUDNESS_SCAN_INSTRUCTIONS = 64

private const val ADD_IMMEDIATE_MASK = 0xffc00000.toInt()
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val ADD_IMMEDIATE_SHIFT = 10
private const val ADD_IMMEDIATE_BITS = 0xfff
private const val LOAD_SINGLE_FROM_BASE_MASK = 0xffffffe0.toInt()
private const val LOAD_SINGLE_FROM_BASE = 0xbd400000.toInt()
private const val BASE_REGISTER_SHIFT = 5

private const val LOCKED_LICENSE_LEVEL = 3.1415f
private const val UNLOCKED_LICENSE_LEVEL = 99.999f

internal val unlockNativeLicenseLevelPatch = rawResourcePatch {
    dependsOn(removePairipVirtualizationPatch)

    execute {
        val library = get(LIBRARY, true)
        if (!library.exists()) throw PatchException("$LIBRARY is missing")

        val bytes = library.readBytes()
        val elf = Arm64ElfReader(bytes)
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val loudnessFunction = elf.symbol(LOUDNESS_SYMBOL)
        val licenseLevelAddress = (0 until LOUDNESS_SCAN_INSTRUCTIONS - 2).firstNotNullOfOrNull { index ->
            val address = loudnessFunction + index * Arm64.INSTRUCTION_BYTES
            val offset = elf.fileOffset(address)
            val adrp = words.getInt(offset)
            val add = words.getInt(offset + Arm64.INSTRUCTION_BYTES)
            val load = words.getInt(offset + 2 * Arm64.INSTRUCTION_BYTES)
            val register = Arm64.destination(adrp)

            val addsToPage = add and ADD_IMMEDIATE_MASK == ADD_IMMEDIATE &&
                Arm64.destination(add) == register && Arm64.baseRegister(add) == register
            val loadsFromPage = load and LOAD_SINGLE_FROM_BASE_MASK ==
                LOAD_SINGLE_FROM_BASE or (register shl BASE_REGISTER_SHIFT)

            if (Arm64.isAdrp(adrp) && addsToPage && loadsFromPage) {
                Arm64.adrpPage(adrp, address) + ((add ushr ADD_IMMEDIATE_SHIFT) and ADD_IMMEDIATE_BITS)
            } else {
                null
            }
        } ?: throw PatchException("$LOUDNESS_SYMBOL: no license level load in its first $LOUDNESS_SCAN_INSTRUCTIONS instructions")

        val licenseLevelOffset = elf.fileOffset(licenseLevelAddress)
        val storedLevel = words.getFloat(licenseLevelOffset)
        if (storedLevel != LOCKED_LICENSE_LEVEL) {
            throw PatchException("$LIBRARY license level at 0x${licenseLevelAddress.toString(16)} is $storedLevel, not $LOCKED_LICENSE_LEVEL")
        }

        words.putFloat(licenseLevelOffset, UNLOCKED_LICENSE_LEVEL)
        library.writeBytes(bytes)
    }
}
