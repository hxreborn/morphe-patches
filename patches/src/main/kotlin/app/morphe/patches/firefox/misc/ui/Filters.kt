/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/Filters.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun filter(
    location: InstructionLocation,
    predicate: (method: Method, instruction: Instruction) -> Boolean,
) = object : InstructionFilter {
    override val location = location
    override fun matches(enclosingMethod: Method, instruction: Instruction) =
        predicate(enclosingMethod, instruction)
}

internal fun callPrefix(
    owner: String,
    namePrefix: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = filter(location) { _, instruction ->
    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
    reference != null && reference.definingClass == owner && reference.name.startsWith(namePrefix)
}

internal fun constAny(
    value: Int,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = filter(location) { _, instruction ->
    instruction is NarrowLiteralInstruction &&
        instruction is OneRegisterInstruction &&
        instruction.narrowLiteral == value
}

internal fun parameterRegister(method: Method, parameterIndex: Int): Int {
    val static = AccessFlags.STATIC.isSet(method.accessFlags)
    var total = if (static) 0 else 1
    var offset = total
    method.parameterTypes.forEachIndexed { i, type ->
        val width = if (type == "J" || type == "D") 2 else 1
        if (i < parameterIndex) offset += width
        total += width
    }
    return method.implementation!!.registerCount - total + offset
}
