/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonvpn.misc.anchors.findPropertyField
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal fun MutableMethod.filterReturnValue(filter: String) {
    findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        replaceInstruction(index, "invoke-static/range { v$register .. v$register }, $filter")
        addInstructions(
            index + 1,
            """
                move-result-object v$register
                check-cast v$register, $returnType
                return-object v$register
            """,
        )
    }
}

private val MutableMethod.thisRegister
    get() = implementation!!.registerCount - parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 } - 1

private fun MutableMethod.fieldAssignment(field: FieldReference): TwoRegisterInstruction? =
    implementation!!.instructions.filterIsInstance<TwoRegisterInstruction>().singleOrNull {
        it.opcode.name.startsWith("iput") && (it as ReferenceInstruction).reference == field &&
            it.registerA > thisRegister
    }

private fun propertyConstructor(toString: Fingerprint, property: String): Pair<MutableMethod, FieldReference> {
    val field = findPropertyField(toString, property)
    val constructor = mutableClassDefBy(field.definingClass).methods.single { method ->
        method.name == "<init>" && method.implementation != null && method.fieldAssignment(field) != null
    }
    return constructor to field
}

private fun MutableMethod.parameterAssignedTo(field: FieldReference) = "p${fieldAssignment(field)!!.registerA - thisRegister}"

internal fun filterConstructorProperty(toString: Fingerprint, property: String, filter: String) {
    val (constructor, field) = propertyConstructor(toString, property)
    val parameter = constructor.parameterAssignedTo(field)
    val filterResult = if (field.type == "Z") {
        "move-result $parameter"
    } else {
        """
            move-result-object $parameter
            check-cast $parameter, ${field.type}
        """
    }
    constructor.addInstructions(
        0,
        """
            invoke-static/range { $parameter .. $parameter }, $filter
            $filterResult
        """,
    )
}

internal fun clearConstructorProperty(toString: Fingerprint, property: String) {
    val (constructor, field) = propertyConstructor(toString, property)
    if (field.type != "Z") throw PatchException("$field is not a boolean")
    constructor.addInstructions(0, "const/16 ${constructor.parameterAssignedTo(field)}, 0x0")
}
