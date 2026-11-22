/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.premium

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ENABLED_SETTING = "$VIBER_SETTINGS_CLASS->unlockViberPlus()Z"
private const val TYPING_SETTING = "$VIBER_SETTINGS_CLASS->unlockTypingPrivacy(Z)Z"

@Suppress("unused")
val unlockViberPlusPatch: Patch = patch(
    name = "Enable Viber Plus features",
    description = "Enables Viber Plus app icons and the typing privacy switch. " +
        "The Plus badge and Invisible Mode need a paid subscription.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("unlockViberPlus", unlockViberPlusPatch)
        AppIconLockedFingerprint.method.returnWhenEnabled(ENABLED_SETTING)

        val typingType = TypingPrivacyControllerFingerprint.match().classDef.type
        val experiment = TypingPrivacyExperimentFingerprint.method
        experiment.returnWhenEnabled(ENABLED_SETTING) { register ->
            "const/16 v$register, 0x1\nreturn v$register"
        }
        val state = typingPrivacyStateFingerprint(typingType, experiment.name).match()
        val (_, _, membershipCall, membershipResult) = state.instructionMatches
        val membership = membershipCall.instruction.getReference<MethodReference>()!!
        val send = typingSendAllowedFingerprint(typingType, experiment.name, membership).match()
        val (_, firstMembershipResult, _, _, secondMembershipResult) = send.instructionMatches
        state.method.addFilterCallAfter(membershipResult.index, TYPING_SETTING)
        send.method.addFilterCallAfter(secondMembershipResult.index, TYPING_SETTING)
        send.method.addFilterCallAfter(firstMembershipResult.index, TYPING_SETTING)
    }
}
