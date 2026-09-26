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
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FIRST_SHADE_REGISTER_OFFSET = 2
private const val LONG_REGISTER_COUNT = 2
private const val SHADE_10_ARGUMENT = 7
private const val SHADE_0_ARGUMENT = 8

internal object BalticSeaPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(ProtonPalette.BALTIC_SEA)),
)

internal object CoreColorsBuilderFingerprint : Fingerprint(
    parameters = listOf("J", "J", "J", "J", "J"),
    custom = { _, classDef -> classDef.type.startsWith(CORE_COMPOSE_THEME_PACKAGE) },
    filters = listOf(methodCall(name = "<init>", opcode = Opcode.INVOKE_DIRECT_RANGE)),
)

private fun RegisterRangeInstruction.shadeRegister(argument: Int) =
    startRegister + FIRST_SHADE_REGISTER_OFFSET + argument * LONG_REGISTER_COUNT

private fun Instruction.readsStaticField(field: FieldReference) =
    opcode == Opcode.SGET_WIDE && getReference<FieldReference>()?.let {
        it.definingClass == field.definingClass && it.name == field.name
    } == true

private fun BytecodePatchContext.returnsStaticField(getter: MethodReference, field: FieldReference) =
    getter.parameterTypes.isEmpty() &&
        classDefByOrNull(getter.definingClass)?.methods
            ?.singleOrNull { it.name == getter.name && it.parameterTypes.isEmpty() }
            ?.implementation?.instructions?.any { it.readsStaticField(field) } == true

private fun BytecodePatchContext.readsBalticSea(method: MutableMethod, index: Int, balticSea: FieldReference): Boolean {
    val write = method.getInstruction(index)
    if (write.readsStaticField(balticSea)) return true
    if (write.opcode != Opcode.MOVE_RESULT_WIDE || index == 0) return false

    val getter = method.getInstruction(index - 1).getReference<MethodReference>() ?: return false
    return returnsStaticField(getter, balticSea)
}

private fun BytecodePatchContext.isDarkColorsBuilder(match: Match, balticSea: FieldReference): Boolean {
    val constructorCall = match.instructionMatches.first()
    val arguments = constructorCall.getInstruction<RegisterRangeInstruction>()
    val shade10Write = match.method.indexOfLastWrite(arguments.shadeRegister(SHADE_10_ARGUMENT), constructorCall.index)
    return readsBalticSea(match.method, shade10Write, balticSea)
}

internal fun BytecodePatchContext.transformCoreDarkBackgrounds() {
    val palette = BalticSeaPaletteFingerprint.matchSingle()
    val balticSeaIndex = palette.instructionMatches.first().index
    val balticSea = palette.method.instructions.drop(balticSeaIndex)
        .first { it.opcode == Opcode.SPUT_WIDE }
        .getReference<FieldReference>()!!
    palette.method.injectColorTransformCall(balticSeaIndex, "$AMOLED_THEME_CLASS->transformBackground(J)J")

    val darkBuilders = CoreColorsBuilderFingerprint.matchAll().filter { isDarkColorsBuilder(it, balticSea) }
    val darkBuilder = darkBuilders.singleOrNull()
        ?: throw PatchException("Expected one Core dark colors builder reading $balticSea, found ${darkBuilders.map { it.method }}")

    val constructorCall = darkBuilder.instructionMatches.first()
    val shade0 = constructorCall.getInstruction<RegisterRangeInstruction>().shadeRegister(SHADE_0_ARGUMENT)
    darkBuilder.method.addInstructions(
        constructorCall.index,
        """
            invoke-static/range { v$shade0 .. v${shade0 + 1} }, $AMOLED_THEME_CLASS->transformPackedBackground(J)J
            move-result-wide v$shade0
        """,
    )
}
