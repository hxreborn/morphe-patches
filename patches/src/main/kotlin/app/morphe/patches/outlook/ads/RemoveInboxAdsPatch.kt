/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val removeInboxAdsPatch: Patch = patch(
    name = "Remove inbox ads",
    description = "Removes sponsored messages from the Focused and Other inbox. Experimental.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeInboxAds", removeInboxAdsPatch)
        val eligibilityCheck = AdsEligibilityCheckFingerprint.match()
        val (resultConstructorCall) = eligibilityCheck.instructionMatches
        val resultConstructor = resultConstructorCall.getInstruction<ReferenceInstruction>().reference as MethodReference

        eligibilityCheck.method.apply {
            check(resultConstructor.definingClass == returnType) {
                "The ad eligibility check builds ${resultConstructor.definingClass}, expected $returnType"
            }
            val resultRegister = findFreeRegister(0)
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->removeInboxAds()Z
                    move-result v$resultRegister
                    if-eqz v$resultRegister, :check_ads
                    new-instance v$resultRegister, $returnType
                    invoke-direct {v$resultRegister}, $resultConstructor
                    return-object v$resultRegister
                """,
                ExternalLabel("check_ads", getInstruction(0)),
            )
        }
    }
}
