/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.piracy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val disablePiracyCheckPatch = bytecodePatch {
    execute {
        PiracyCheckerStartFingerprint.matchSingle().method.apply {
            val allowCallbackField = getInstruction(
                indexOfFirstInstructionOrThrow(Opcode.IGET_OBJECT),
            ).getReference<FieldReference>()!!
            val allowMethod = getInstruction(
                indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.let {
                            it.definingClass == allowCallbackField.type && it.parameterTypes.isEmpty()
                        } == true
                },
            ).getReference<MethodReference>()!!

            addInstructions(
                0,
                """
                    move-object/from16 v0, p0
                    iget-object v0, v0, ${descriptor(allowCallbackField)}
                    if-eqz v0, :skip
                    invoke-virtual { v0 }, ${descriptor(allowMethod)}
                    :skip
                    return-void
                """,
            )
        }
    }
}
