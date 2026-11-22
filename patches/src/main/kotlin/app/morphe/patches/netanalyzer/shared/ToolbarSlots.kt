/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val OBJECT_TYPE = "Ljava/lang/Object;"
private const val INT_TYPE = "I"
private const val BOOLEAN_TYPE = "Z"
private const val GET_CLASS = "getClass"
private const val GROUP_KEY_LOAD_DISTANCE = 2
private const val GROUP_END_LOAD_DISTANCE = 2

internal class ToolbarSlot(
    val method: MutableMethod,
    val classDef: MutableClass,
    val index: Int,
    val composerRegister: Int,
    val composerType: String,
)

internal fun emptyGroupSlot(composableName: String): ToolbarSlot =
    emptyGroupSlotOrNull(composableFingerprint(composableName).match())
        ?: throw PatchException("$composableName has no empty composer group after a null check")

internal fun emptyGroupSlotOrNull(match: Match): ToolbarSlot? {
    val method = match.method
    val instructions = method.instructions.toList()
    fun groupCall(index: Int, parameterType: String) = instructions.getOrNull(index)
        ?.takeIf { it.opcode == Opcode.INVOKE_VIRTUAL }
        ?.getReference<MethodReference>()
        ?.takeIf { it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf(parameterType) }
    fun isGetClass(index: Int) = instructions[index].let { instruction ->
        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
            instruction.getReference<MethodReference>()!!.let { it.definingClass == OBJECT_TYPE && it.name == GET_CLASS }
    }
    val (composableSource) = match.instructionMatches
    (composableSource.index until instructions.size).filter(::isGetClass).forEach { getClass ->
        (getClass + 1..getClass + GROUP_KEY_LOAD_DISTANCE).forEach { index ->
            val key = groupCall(index, INT_TYPE) ?: return@forEach
            val closes = (index + 1..index + GROUP_END_LOAD_DISTANCE).any {
                groupCall(it, BOOLEAN_TYPE)?.definingClass == key.definingClass
            }
            if (closes) {
                val keyCall = instructions[index] as FiveRegisterInstruction
                return ToolbarSlot(method, match.classDef, index + 1, keyCall.registerC, key.definingClass)
            }
        }
    }
    return null
}
