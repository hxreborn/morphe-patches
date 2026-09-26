/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.patches.shared.misc.proton.indexOfLastWrite
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val THIS_REGISTER_COUNT = 1
private const val LONG_REGISTER_COUNT = 2
private const val COLORS_ASSIGNMENT_DISTANCE = 4

internal const val DARK_COLORS_FIELD = "Dark"
internal const val LIGHT_COLORS_FIELD = "Light"

private fun MutableMethod.indexOfPassColorsConstructorCall(colorsField: String): Int {
    val instructions = instructions.toList()
    val calls = instructions.indices.filter { index ->
        val instruction = instructions[index]
        instruction.opcode == Opcode.INVOKE_DIRECT_RANGE &&
            instruction.getReference<MethodReference>()?.name == "<init>" &&
            instructions.drop(index + 1).take(COLORS_ASSIGNMENT_DISTANCE).any {
                it.opcode == Opcode.SPUT_OBJECT && it.getReference<FieldReference>()?.name == colorsField
            }
    }
    return calls.singleOrNull()
        ?: throw PatchException("Expected one PassColors constructor call assigned to $colorsField, found ${calls.size}")
}

private tailrec fun MutableMethod.paletteFieldWrittenTo(register: Int, before: Int): String {
    val index = indexOfLastWrite(register, before)
    val instruction = getInstruction(index)
    return when (instruction.opcode) {
        Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE_16 ->
            paletteFieldWrittenTo((instruction as TwoRegisterInstruction).registerB, index)
        Opcode.SGET_WIDE -> instruction.getReference<FieldReference>()!!.name
        else -> throw PatchException(
            "Expected a palette read into register v$register at instruction $index, found ${instruction.opcode}",
        )
    }
}

internal fun MutableMethod.transformPassColors(
    colorsField: String,
    roles: Map<Int, String>,
    transform: String,
) {
    val callIndex = indexOfPassColorsConstructorCall(colorsField)
    val arguments = getInstruction<RegisterRangeInstruction>(callIndex)
    val registers = roles.map { (role, paletteField) ->
        val register = arguments.startRegister + THIS_REGISTER_COUNT + role * LONG_REGISTER_COUNT
        val field = paletteFieldWrittenTo(register, callIndex)
        if (field != paletteField) {
            throw PatchException("PassColors.$colorsField role $role reads PassPalette.$field, expected $paletteField")
        }
        register
    }

    addInstructions(
        callIndex,
        registers.joinToString("\n") {
            """
                invoke-static/range { v$it .. v${it + 1} }, $transform
                move-result-wide v$it
            """
        },
    )
}
