/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.airexplorer.misc.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.arm64.Arm64ElfReader
import app.morphe.patches.shared.misc.dotnet.editManagedAssembly
import app.morphe.patches.shared.misc.requireArm64
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val APP_ASSEMBLY = "AirExplorer.dll"
private const val AOT_LIBRARY = "lib/arm64-v8a/libaot-AirExplorer.dll.so"
private const val EXPLORER_NAMESPACE = "AirExplorer.ViewModels"
private const val EXPLORER_TYPE = "ExplorerPageViewModel"
private const val IS_FREE_GETTER = "get_IsFree"
private const val LOAD_FALSE: Byte = 0x16
private const val RETURN: Byte = 0x2a
private val RETURN_FALSE = byteArrayOf(LOAD_FALSE, RETURN)

private const val AOT_INFO_SYMBOL = "mono_aot_file_info"
private const val AOT_FILE_VERSION = 187
private const val AOT_METHOD_ADDRESSES_POINTER_OFFSET = 0x40L
private const val AOT_METHOD_COUNT_OFFSET = 0x154
private const val AOT_CALL_TABLE_ENTRY_SIZE_OFFSET = 0x188
private const val EXPECTED_METHOD_PROLOGUE = 0xa9be7bfd.toInt()
private const val EXPECTED_FRAME_POINTER_SETUP = 0x910003fd.toInt()
private const val MOVE_ZERO_TO_W0 = 0x52800000
private const val RETURN_FROM_FUNCTION = 0xd65f03c0.toInt()

private class MonoAotImage(private val bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val elf = Arm64ElfReader(bytes)

    fun replaceMethodWithFalse(methodIndex: Int): ByteArray {
        val infoAddress = elf.symbol(AOT_INFO_SYMBOL)
        val infoOffset = elf.fileOffset(infoAddress)
        val version = buffer.getInt(infoOffset)
        if (version != AOT_FILE_VERSION) {
            throw PatchException("$AOT_INFO_SYMBOL version is $version, expected $AOT_FILE_VERSION")
        }

        val methodCount = buffer.getInt(infoOffset + AOT_METHOD_COUNT_OFFSET)
        if (methodIndex !in 0 until methodCount) {
            throw PatchException("Method index $methodIndex is outside $methodCount AOT methods")
        }
        val callInstructionSize = buffer.getInt(infoOffset + AOT_CALL_TABLE_ENTRY_SIZE_OFFSET)
        if (callInstructionSize != Arm64.INSTRUCTION_BYTES) {
            throw PatchException(
                "AOT call table entries are $callInstructionSize bytes, expected ${Arm64.INSTRUCTION_BYTES}",
            )
        }

        val callTableAddress = elf.relativeRelocationAddend(infoAddress + AOT_METHOD_ADDRESSES_POINTER_OFFSET)
        val callInstructionAddress = callTableAddress + methodIndex * callInstructionSize
        val callInstruction = buffer.getInt(elf.fileOffset(callInstructionAddress))
        if (!Arm64.isBranchLink(callInstruction)) {
            throw PatchException("AOT method $methodIndex call table entry is not a branch with link")
        }
        val methodAddress = Arm64.branchLinkTarget(callInstruction, callInstructionAddress.toInt()).toLong()
        val methodOffset = elf.fileOffset(methodAddress)
        val prologue = buffer.getInt(methodOffset)
        val framePointerSetup = buffer.getInt(methodOffset + Arm64.INSTRUCTION_BYTES)
        if (prologue != EXPECTED_METHOD_PROLOGUE || framePointerSetup != EXPECTED_FRAME_POINTER_SETUP) {
            throw PatchException("AOT method $methodIndex does not start with the expected instructions")
        }
        buffer.putInt(methodOffset, MOVE_ZERO_TO_W0)
        buffer.putInt(methodOffset + Arm64.INSTRUCTION_BYTES, RETURN_FROM_FUNCTION)
        return bytes
    }
}

@Suppress("unused")
val removeProPromotionPatch = patch(
    name = "Remove Pro promotion",
    description = "Removes the Pro promotion from the file browser.",
) {
    compatibleWith(AppCompatibilities.AIR_EXPLORER)
    availability(requireArm64)

    execute {
        val methodIndexes = mutableSetOf<Int>()
        editManagedAssembly(APP_ASSEMBLY) {
            methodIndexes += methodTokenIndex(EXPLORER_NAMESPACE, EXPLORER_TYPE, IS_FREE_GETTER)
            replaceMethodBody(EXPLORER_NAMESPACE, EXPLORER_TYPE, IS_FREE_GETTER, RETURN_FALSE)
        }
        val methodIndex = methodIndexes.singleOrNull()
            ?: throw PatchException("$APP_ASSEMBLY method indexes differ between architectures: $methodIndexes")

        val library = resourceFile(AOT_LIBRARY, true)
        if (!library.exists()) throw PatchException("$AOT_LIBRARY is missing")
        library.writeBytes(MonoAotImage(library.readBytes()).replaceMethodWithFalse(methodIndex))
    }
}
