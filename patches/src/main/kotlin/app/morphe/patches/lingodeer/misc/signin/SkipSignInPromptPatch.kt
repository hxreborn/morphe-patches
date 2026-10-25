/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lingodeer.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val skipSignInPromptPatch = bytecodePatch(
    name = "Skip sign-in prompt",
    description = "Opens lessons past the second unit without an account.",
) {
    compatibleWith(AppCompatibilities.LINGODEER)

    availability(requireArm)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, spoofSignaturePatch)

    execute {
        val promptBypassFlag = SignInPromptFingerprint.matchAll().map { match ->
            match.instructionMatches.single { it.filter === promptBypassFlagRead }
                .getInstruction<ReferenceInstruction>().reference as FieldReference
        }.distinct().single()

        mutableClassDefBy(promptBypassFlag.definingClass)
            .methods.single { it.name == "<clinit>" }
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    sput-boolean v0, $promptBypassFlag
                """,
            )
    }
}
