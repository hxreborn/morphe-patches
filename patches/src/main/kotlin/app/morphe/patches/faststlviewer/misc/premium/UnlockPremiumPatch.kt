/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.faststlviewer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PRODUCT_READER_COUNT = 2
private const val PRO_READER_COUNT = 4

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks slice view, colors, lighting, normals, measurements, " +
        "printability analysis and transform, and removes ads.",
) {
    compatibleWith(AppCompatibilities.FAST_STL_VIEWER)

    execute {
        forceBooleanFieldReadsTrue(ProductButtonTextFingerprint, PRODUCT_READER_COUNT)
        forceBooleanFieldReadsTrue(PurchasesUpdatedFingerprint, PRO_READER_COUNT)
        RequestAdConsentFingerprint.matchSingle().method.returnEarly()
    }
}

private fun BytecodePatchContext.forceBooleanFieldReadsTrue(
    fieldAccessFingerprint: Fingerprint,
    readerCount: Int,
) {
    val anchor = fieldAccessFingerprint.matchSingle()
    val (fieldAccess) = anchor.instructionMatches
    val field = fieldAccess.instruction.getReference<FieldReference>()!!
    booleanFieldReadFingerprint(field).matchAll(anchor.originalClassDef, readerCount..readerCount).forEach { reader ->
        val method = reader.method
        method.findInstructionIndicesReversed {
            opcode == Opcode.IGET_BOOLEAN && getReference<FieldReference>() == field
        }.forEach { index ->
            val valueRegister = method.getInstruction<TwoRegisterInstruction>(index).registerA
            method.addInstruction(index + 1, "const/4 v$valueRegister, 0x1")
        }
    }
}
