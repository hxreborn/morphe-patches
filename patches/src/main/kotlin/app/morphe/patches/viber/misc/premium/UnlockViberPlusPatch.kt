/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.premium

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
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
val unlockViberPlusPatch: BytecodePatch = bytecodePatch(
    name = "Enable Viber Plus features",
    description = "Enables Viber Plus app icons and the typing privacy switch. " +
        "The Plus badge and Invisible Mode need a paid subscription.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("unlockViberPlus", unlockViberPlusPatch)
        AppIconLockedFingerprint.matchSingle().method.returnWhenEnabled(ENABLED_SETTING)

        val typingType = TypingPrivacyControllerFingerprint.matchSingle().classDef.type
        val experiment = TypingPrivacyExperimentFingerprint.matchSingle().method
        experiment.returnWhenEnabled(ENABLED_SETTING) { register ->
            "const/16 v$register, 0x1\nreturn v$register"
        }
        val state = typingPrivacyStateFingerprint(typingType, experiment.name).matchSingle()
        val (_, _, membershipCall, membershipResult) = state.instructionMatches
        val membership = membershipCall.instruction.getReference<MethodReference>()!!
        val send = typingSendAllowedFingerprint(typingType, experiment.name, membership).matchSingle()
        val (_, firstMembershipResult, _, _, secondMembershipResult) = send.instructionMatches
        state.method.addFilterCallAfter(membershipResult.index, TYPING_SETTING)
        send.method.addFilterCallAfter(secondMembershipResult.index, TYPING_SETTING)
        send.method.addFilterCallAfter(firstMembershipResult.index, TYPING_SETTING)
    }
}
