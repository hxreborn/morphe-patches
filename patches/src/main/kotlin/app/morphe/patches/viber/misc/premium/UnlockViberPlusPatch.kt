/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ENABLED_SETTING = "$VIBER_SETTINGS_CLASS->unlockViberPlus()Z"
private const val TYPING_SETTING = "$VIBER_SETTINGS_CLASS->unlockTypingPrivacy(Z)Z"

@Suppress("unused")
val unlockViberPlusPatch: BytecodePatch = bytecodePatch(
    name = "Unlock Viber Plus",
    description = "Unlocks Viber Plus app icons and the typing privacy switch. " +
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
        val (_, _, membershipCall) = state.instructionMatches
        val membership = state.method.getInstruction(membershipCall.index).getReference<MethodReference>()!!
        val send = typingSendAllowedFingerprint(typingType, experiment.name, membership).matchSingle()
        val (firstMembershipCall, _, secondMembershipCall) = send.instructionMatches
        listOf(
            state.method to listOf(membershipCall.index),
            send.method to listOf(firstMembershipCall.index, secondMembershipCall.index),
        ).forEach { (method, calls) ->
            calls.sortedDescending().forEach { callIndex ->
                val resultIndex = callIndex + 1
                val result = method.getInstruction<OneRegisterInstruction>(resultIndex)
                require(result.opcode == Opcode.MOVE_RESULT) {
                    "Membership call at $callIndex in ${method.name} is followed by ${result.opcode}, expected move-result"
                }
                val register = result.registerA
                method.addInstructions(
                    resultIndex + 1,
                    """
                        invoke-static/range { v$register .. v$register }, $TYPING_SETTING
                        move-result v$register
                    """,
                )
            }
        }
    }
}
