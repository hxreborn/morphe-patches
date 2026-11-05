/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.lingodeer.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PROMPT_METHOD_COUNT = 2
private const val BYPASS_CONSTANT_COUNT = 3

@Suppress("unused")
val skipSignInPromptPatch = bytecodePatch(
    name = "Skip sign-in prompt",
    description = "Opens lessons past the second unit without an account.",
) {
    compatibleWith(AppCompatibilities.LINGODEER)

    availability(requireArm)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, spoofSignaturePatch)

    execute {
        val (_, loginWrite) = LessonAccessFingerprint.matchSingle().instructionMatches
        val requireLogin = loginWrite.instruction.getReference<FieldReference>()!!
        val promptMatches = signInPromptFingerprint(requireLogin).matchAll(PROMPT_METHOD_COUNT..PROMPT_METHOD_COUNT)
        val bypassFlag = promptMatches.map { match ->
            val (bypassRead) = match.instructionMatches
            bypassRead.instruction.getReference<FieldReference>()!!
        }.distinct().single()
        val bypassConstants = promptMatches
            .flatMap { match ->
                match.method.findInstructionIndicesReversed {
                    opcode == Opcode.SGET_BOOLEAN && getReference<FieldReference>() == bypassFlag
                }.map { index -> match.method to index + 1 }
            }
        check(bypassConstants.size == BYPASS_CONSTANT_COUNT) {
            "Expected $BYPASS_CONSTANT_COUNT lesson sign-in bypass constants, found ${bypassConstants.size}"
        }
        bypassConstants.forEach { (method, index) ->
            val constant = method.getInstruction<OneRegisterInstruction>(index)
            val field = constant.getReference<FieldReference>()!!
            check(field.definingClass == "Ljava/lang/Boolean;" && field.name == "FALSE") {
                "Expected disabled lesson sign-in bypass after $bypassFlag, found $field"
            }
            method.replaceInstruction(
                index,
                "sget-object v${constant.registerA}, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;",
            )
        }
    }
}
