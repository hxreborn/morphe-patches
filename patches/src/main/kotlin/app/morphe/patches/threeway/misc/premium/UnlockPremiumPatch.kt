/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.threeway.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val GOLD = 1

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Gold: unblurred photos of the people who liked or visited you, " +
        "the Gold crown, and the advanced search filters.",
) {
    compatibleWith(AppCompatibilities.THREEWAY)

    execute {
        val profileFieldSetter = ProfileFieldSetterFingerprint.matchSingle()
        val profileType = profileFieldSetter.classDef.type
        val setGold = profileFieldSetter.instructionMatches.last().instruction.getReference<MethodReference>()!!

        myProfileFingerprint(profileType).matchSingle().method.apply {
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val profileRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            val goldRegister = getFreeRegisterProvider(returnIndex, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                returnIndex,
                """
                    if-eqz v$profileRegister, :done
                    const/4 v$goldRegister, $GOLD
                    invoke-virtual { v$profileRegister, v$goldRegister }, ${descriptor(setGold)}
                """,
                ExternalLabel("done", getInstruction(returnIndex)),
            )
        }
    }
}
