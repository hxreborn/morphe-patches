/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

context(_: BytecodePatchContext)
internal fun removeComposableCall(label: String, composable: Method) {
    val call = Fingerprint(
        filters = listOf(decryptedString(label), methodCall(reference = composable)),
    ).matchSingle()
    call.method.replaceInstruction(call.instructionMatches.last().index, "nop")
}

internal fun MutableMethod.insertBeforeStringLoad(index: Int, smali: String) {
    val load = getInstruction<OneRegisterInstruction>(index)
    if (load.opcode != Opcode.CONST_STRING && load.opcode != Opcode.CONST_STRING_JUMBO) {
        throw PatchException("Instruction $index of $name is ${load.opcode}, not a string load")
    }
    val string = (load as Instruction).getReference<StringReference>()!!.string
    replaceInstruction(index, smali)
    addInstruction(index + 1, "const-string v${load.registerA}, \"${string.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
}

private const val NIBBLE_REGISTER_LIMIT = 15

internal fun requireNibbleRegisters(vararg registers: Int) {
    registers.firstOrNull { it > NIBBLE_REGISTER_LIMIT }?.let {
        throw PatchException("Register v$it does not fit a non-range invoke")
    }
}
