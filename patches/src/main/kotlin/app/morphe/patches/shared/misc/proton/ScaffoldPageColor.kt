/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DEFAULT_COLOR_READ_WINDOW = 7
private const val BRIDGE_LOCALS = 3

internal fun BytecodePatchContext.captureScaffoldPageColor(
    match: Match,
    backgroundParameter: Int,
    backgroundDefault: Int,
    colorGetterName: String? = null,
) {
    val (callMatch) = match.instructionMatches
    val call = callMatch.instruction as RegisterRangeInstruction
    val native = call.getReference<MethodReference>()!!
    val parameters = native.parameterTypes.map(CharSequence::toString)
    check(parameters[backgroundParameter] == "J" && parameters.last() == "I")
    val sdk = classDefBy(native.definingClass).methods.single {
        it.name == native.name && it.parameterTypes == native.parameterTypes
    }
    val instructions = sdk.implementation!!.instructions.toList()
    val readIndices = if (colorGetterName != null) {
        instructions.indices.filter { instructions[it].getReference<MethodReference>()?.name == colorGetterName }
    } else {
        instructions.indices.filter {
            (instructions[it] as? NarrowLiteralInstruction)?.narrowLiteral == backgroundDefault &&
                instructions[it].opcode in listOf(Opcode.AND_INT_LIT8, Opcode.AND_INT_LIT16)
        }.mapNotNull { index ->
            (index + 1 until minOf(instructions.size, index + DEFAULT_COLOR_READ_WINDOW)).firstOrNull {
                instructions[it].opcode == Opcode.IGET_WIDE
            }
        }
    }
    val readIndex = readIndices.distinctBy { index ->
        instructions[index].getReference<MethodReference>()?.let(::descriptor)
            ?: descriptor(instructions[index].getReference<FieldReference>()!!)
    }.single()
    val getter = instructions[readIndex].getReference<MethodReference>()
    val field = instructions[readIndex].getReference<FieldReference>()
    val colorType = getter?.definingClass ?: field!!.definingClass
    val consumeIndex = (readIndex - 1 downTo 0).first {
        instructions[it].getReference<MethodReference>()?.returnType == "Ljava/lang/Object;"
    }
    val consumeCall = instructions[consumeIndex] as FiveRegisterInstruction
    val consume = instructions[consumeIndex].getReference<MethodReference>()!!
    check(consume.parameterTypes.size == 1 && instructions[consumeIndex].opcode == Opcode.INVOKE_VIRTUAL)
    var localRegister = consumeCall.registerD
    val local = (consumeIndex - 1 downTo 0).firstNotNullOf { index ->
        val instruction = instructions[index]
        if (instruction !is OneRegisterInstruction || !instruction.opcode.setsRegister() || instruction.registerA != localRegister) {
            null
        } else if (instruction.opcode == Opcode.SGET_OBJECT) {
            instruction.getReference<FieldReference>()!!
        } else {
            check(instruction is TwoRegisterInstruction && instruction.opcode in listOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16)) {
                "Unexpected scaffold theme local assignment in ${descriptor(native)}"
            }
            localRegister = instruction.registerB
            null
        }
    }
    val readColor = if (getter != null) {
        "invoke-virtual { v0 }, ${descriptor(getter)}\nmove-result-wide v0"
    } else {
        "iget-wide v0, v0, ${descriptor(field!!)}"
    }
    val composerParameter = parameters.dropLastWhile { it == "I" }.lastIndex
    fun register(parameter: Int) = parameters.take(parameter).sumOf { if (it == "J" || it == "D") 2 else 1 }
    val count = register(parameters.size)
    val bridge = mutableClassDefBy(NAVIGATION_BAR_BACKDROP_CLASS).addStaticMethod(
        "mainScaffold", parameters, "V", count + BRIDGE_LOCALS,
        """
            move/from16 v2, p${count - 1}
            and-int/lit16 v2, v2, $backgroundDefault
            if-eqz v2, :explicit_color
            sget-object v0, ${descriptor(local)}
            move-object/from16 v1, p${register(composerParameter)}
            check-cast v1, ${consume.definingClass}
            invoke-virtual { v1, v0 }, ${descriptor(consume)}
            move-result-object v0
            check-cast v0, $colorType
            $readColor
            goto :capture
            :explicit_color
            move-wide/from16 v0, p${register(backgroundParameter)}
            :capture
            invoke-static { v0, v1 }, $NAVIGATION_BAR_BACKDROP_CLASS->pageColor(J)J
            invoke-static/range { p0 .. p${count - 1} }, ${descriptor(native)}
            return-void
        """,
    )
    match.method.replaceInstruction(
        callMatch.index,
        "invoke-static/range { v${call.startRegister} .. v${call.startRegister + call.registerCount - 1} }, $bridge",
    )
}
