/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val VM_RUNNER = "Lcom/pairip/VMRunner;"
private const val PAIRIP_PACKAGE = "Lcom/pairip/"

private fun Method.callsVmRunner() = implementation?.instructions?.any {
    it.getReference<MethodReference>()?.definingClass == VM_RUNNER
} == true

internal fun ClassDef.virtualizedMethods() =
    if (type.startsWith(PAIRIP_PACKAGE)) emptyList() else methods.filter { it.callsVmRunner() }

private fun Method.requireVirtualized() = check(callsVmRunner()) {
    "${descriptor(this)} is not a PairIP stub in this build"
}

internal fun MutableClass.replaceVirtualizedMethod(name: String, registers: Int, smali: String) {
    methods.single { it.name == name }.requireVirtualized()
    replaceMethod(name, registers, smali)
}

internal class RestoredMethod(val type: String, val name: String, val registers: Int, val smali: String)

internal fun BytecodePatchContext.restoreVirtualizedMethods(methods: List<RestoredMethod>) = methods.forEach { restored ->
    mutableClassDefBy(restored.type).replaceVirtualizedMethod(restored.name, restored.registers, restored.smali)
}

internal fun MutableMethod.replaceVirtualizedBody(smali: String) {
    requireVirtualized()
    removeInstructions(0, instructions.size)
    addInstructions(0, smali)
}
