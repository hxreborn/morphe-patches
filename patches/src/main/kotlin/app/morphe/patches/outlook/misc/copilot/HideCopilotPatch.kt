/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.copilot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val NOT_SUPPORTED_STATE = "NotSupported"

private fun Fingerprint.returnWhenCopilotHidden(hiddenResult: (register: Int, returnType: String) -> String) {
    method.apply {
        val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->hideCopilot()Z
                move-result v$register
                if-eqz v$register, :show_copilot
                ${hiddenResult(register, returnType)}
            """,
            ExternalLabel("show_copilot", getInstruction(0)),
        )
    }
}

private val notSupported = { register: Int, stateType: String ->
    classDefBy(stateType).fields.singleOrNull { it.name == NOT_SUPPORTED_STATE }
        ?: throw PatchException("$stateType has no $NOT_SUPPORTED_STATE constant")
    """
        sget-object v$register, $stateType->$NOT_SUPPORTED_STATE:$stateType
        return-object v$register
    """
}

private val unavailable = { register: Int, _: String ->
    """
        const/4 v$register, 0x0
        return v$register
    """
}

@Suppress("unused")
val hideCopilotPatch: Patch = patch(
    name = "Hide Copilot",
    description = "Hides Copilot chat, summaries, scheduling and writing help on all accounts, " +
        "regardless of the Copilot setting in Outlook.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("hideCopilot", hideCopilotPatch)
        CopilotTypeSupportedFingerprint.returnWhenCopilotHidden(notSupported)
        CopilotCapabilitySupportedFingerprint.returnWhenCopilotHidden(notSupported)
        CopilotTypeEnabledFingerprint.returnWhenCopilotHidden(unavailable)
        CopilotCapabilityEnabledFingerprint.returnWhenCopilotHidden(unavailable)
        FullCopilotAccountAvailableFingerprint.returnWhenCopilotHidden(unavailable)
        listOf(scheduleWithCopilotPartnerFingerprint, forwardToCopilotPartnerFingerprint).forEach { partner ->
            val requirement = partner.match()
            val (alwaysOnCall, requirementResult) = requirement.instructionMatches
            val factory = requirement.method.getInstruction<FiveRegisterInstruction>(alwaysOnCall.index).registerC
            val resultRegister = requirementResult.getInstruction<OneRegisterInstruction>().registerA
            requirement.method.addInstructionsWithLabels(
                alwaysOnCall.index,
                """
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->hideCopilot()Z
                    move-result v$resultRegister
                    if-eqz v$resultRegister, :load_partner
                    invoke-interface {v$factory}, $FEATURE_REQUIREMENT_FACTORY_CLASS->alwaysOff()${requirement.method.returnType}
                    move-result-object v$resultRegister
                    return-object v$resultRegister
                """,
                ExternalLabel("load_partner", requirement.method.getInstruction(alwaysOnCall.index)),
            )
        }
    }
}
