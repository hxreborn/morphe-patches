/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.streak

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}streak/StreakPrompts;"

@Suppress("unused")
val removeStreakPromptsPatch = bytecodePatch(
    name = "Remove streak prompts",
    description = "Removes the offers to recover your streak and the Save your streak sheet. Your streak count doesn't change.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        listOf(
            StreakRecoveryOfferEligibleToStringFingerprint,
            StreakSaveOfferEligibleToStringFingerprint,
        ).forEach { eligibleToString ->
            val eligible = eligibleToString.matchSingle().classDef
            val check = eligibilityCheckFingerprint(eligible.type).matchSingle().method
            val notEligible = notEligibleSingletonFingerprint(eligible.interfaces).matchSingle()
                .instructionMatches.single().getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!
            notEligibleReadFingerprint(notEligible).matchOrNull(check)
                ?: throw PatchException("${check.definingClass}->${check.name} does not read $notEligible")

            val result = check.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            check.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS->removed()Z
                    move-result v$result
                    if-eqz v$result, :continue
                    sget-object v$result, ${notEligible.definingClass}->${notEligible.name}:${notEligible.type}
                    return-object v$result
                """,
                ExternalLabel("continue", check.getInstruction(0)),
            )
        }
    }
}
