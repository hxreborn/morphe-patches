/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.faststlviewer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks slice view, colors, lighting, normals, measurements, " +
        "printability analysis and transform, and removes ads.",
) {
    compatibleWith(AppCompatibilities.FAST_STL_VIEWER)

    execute {
        forceBooleanFieldReadsTrue(ProductButtonTextFingerprint)
        forceBooleanFieldReadsTrue(PurchasesUpdatedFingerprint)
        RequestAdConsentFingerprint.matchSingle().method.returnEarly()
    }
}

private fun BytecodePatchContext.forceBooleanFieldReadsTrue(fieldAccessFingerprint: Fingerprint) {
    val anchor = fieldAccessFingerprint.matchSingle()
    val field = anchor.instructionMatches.first().instruction.getReference<FieldReference>()!!
    booleanFieldReadFingerprint(field).matchAll(anchor.originalClassDef).forEach { reader ->
        val method = reader.method
        method.instructions.withIndex()
            .filter { (_, read) -> read.opcode == Opcode.IGET_BOOLEAN && read.getReference<FieldReference>() == field }
            .reversed()
            .forEach { (index, read) ->
                val valueRegister = (read as TwoRegisterInstruction).registerA
                method.addInstruction(index + 1, "const/4 v$valueRegister, 0x1")
            }
    }
}
