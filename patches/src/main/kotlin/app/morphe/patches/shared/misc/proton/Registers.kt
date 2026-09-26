/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

internal fun MutableMethod.indexOfLastWrite(register: Int, before: Int) =
    indexOfFirstInstructionReversed(before - 1) { this is OneRegisterInstruction && registerA == register }
        .takeIf { it >= 0 }
        ?: throw PatchException("No write to register v$register before instruction $before")

internal fun MutableMethod.literalWrittenTo(register: Int, before: Int): Long {
    val index = indexOfLastWrite(register, before)
    val instruction = getInstruction(index)
    if (instruction !is WideLiteralInstruction) {
        throw PatchException(
            "Expected a literal write to register v$register at instruction $index, found ${instruction.opcode}",
        )
    }
    return instruction.wideLiteral
}
