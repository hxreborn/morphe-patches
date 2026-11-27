/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FIRST_SHADE_REGISTER_OFFSET = 2
private const val LONG_REGISTER_COUNT = 2
private const val SHADE_15_ARGUMENT = 6
private const val SHADE_10_ARGUMENT = 7
private const val SHADE_0_ARGUMENT = 8

internal object BalticSeaPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(ProtonPalette.BALTIC_SEA)),
)

private fun coreDarkColorsBuilderFingerprint(balticSea: FieldReference): Fingerprint {
    val constructorCall = methodCall(name = "<init>", opcode = Opcode.INVOKE_DIRECT_RANGE)
    return Fingerprint(
        parameters = listOf("J", "J", "J", "J", "J"),
        filters = listOf(constructorCall),
        custom = { method, _ ->
            val constructorIndex = method.indexOfFirstInstruction(constructorCall)
            constructorIndex >= 0 && method.shadeReads(constructorIndex, SHADE_10_ARGUMENT, balticSea)
        },
    )
}

private fun RegisterRangeInstruction.shadeRegister(argument: Int) =
    startRegister + FIRST_SHADE_REGISTER_OFFSET + argument * LONG_REGISTER_COUNT

private fun Instruction.readsStaticField(field: FieldReference) =
    opcode == Opcode.SGET_WIDE && getReference<FieldReference>()?.let {
        it.definingClass == field.definingClass && it.name == field.name
    } == true

private fun returnsStaticField(getter: MethodReference, field: FieldReference) =
    getter.parameterTypes.isEmpty() &&
        classDefByOrNull(getter.definingClass)?.methods
            ?.singleOrNull { it.name == getter.name && it.parameterTypes.isEmpty() }
            ?.implementation?.instructions?.any { it.readsStaticField(field) } == true

private fun Method.readsPaletteField(index: Int, field: FieldReference): Boolean {
    val write = getInstruction(index)
    if (write.readsStaticField(field)) return true
    if (write.opcode != Opcode.MOVE_RESULT_WIDE || index == 0) return false

    val getter = getInstruction(index - 1).getReference<MethodReference>() ?: return false
    return returnsStaticField(getter, field)
}

private fun Method.shadeReads(constructorIndex: Int, argument: Int, field: FieldReference): Boolean {
    val arguments = getInstruction<RegisterRangeInstruction>(constructorIndex)
    val shadeWrite = indexOfLastWriteOrNull(arguments.shadeRegister(argument), constructorIndex) ?: return false
    return readsPaletteField(shadeWrite, field)
}

internal fun MutableMethod.paletteFieldAssignedFrom(literal: Long): FieldReference {
    val literalIndex = indexOfFirstLiteralInstructionOrThrow(literal)
    return instructions.drop(literalIndex)
        .first { it.opcode == Opcode.SPUT_WIDE }
        .getReference<FieldReference>()!!
}

private fun MutableMethod.transformShade(constructorCall: Match.InstructionMatch, argument: Int, transform: String) {
    val shade = constructorCall.getInstruction<RegisterRangeInstruction>().shadeRegister(argument)
    addInstructions(
        constructorCall.index,
        """
            invoke-static/range { v$shade .. v${shade + 1} }, $AMOLED_THEME_CLASS->$transform(J)J
            move-result-wide v$shade
        """,
    )
}

internal fun transformCoreDarkBackgrounds() {
    val palette = BalticSeaPaletteFingerprint.match()
    val balticSea = palette.method.paletteFieldAssignedFrom(ProtonPalette.BALTIC_SEA)
    val bastille = palette.method.paletteFieldAssignedFrom(ProtonPalette.BASTILLE)
    palette.method.injectColorTransformCall(
        palette.instructionMatches.first().index,
        "$AMOLED_THEME_CLASS->transformBackground(J)J",
    )

    val darkBuilder = coreDarkColorsBuilderFingerprint(balticSea).matchAll(1..1).single()
    val constructorCall = darkBuilder.instructionMatches.single()
    if (!darkBuilder.originalMethod.shadeReads(constructorCall.index, SHADE_15_ARGUMENT, bastille)) {
        throw PatchException("Core dark colors builder ${darkBuilder.originalMethod} does not read $bastille as shade15")
    }

    darkBuilder.method.apply {
        transformShade(constructorCall, SHADE_15_ARGUMENT, "transformPackedRaisedSurface")
        transformShade(constructorCall, SHADE_0_ARGUMENT, "transformPackedBackground")
    }
}
