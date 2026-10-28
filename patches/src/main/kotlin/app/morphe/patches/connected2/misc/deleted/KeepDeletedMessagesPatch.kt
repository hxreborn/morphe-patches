/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.deleted

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/connected2/DeletedMessages;"

@Suppress("unused")
val keepDeletedMessagesPatch = bytecodePatch(
    name = "Keep deleted messages",
    description = "Keeps messages the other person deletes and marks deleted text messages with 🗑.",
) {
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    extendWith("extensions/extension.mpe")

    execute {
        DeleteMessageHandlerFingerprint.matchSingle().apply {
            val lookup = instructionMatches[2].index
            val ageCheck = instructionMatches.last().index
            val message = method.getInstruction<OneRegisterInstruction>(lookup + 1).registerA
            val skipDelete = method.getInstruction<BuilderOffsetInstruction>(ageCheck).target.location.instruction!!

            method.addInstructionsWithLabels(
                ageCheck + 1,
                """
                    invoke-static/range { v$message .. v$message }, $EXTENSION_CLASS->mark(Ljava/lang/Object;)V
                    goto :skip_delete
                """,
                ExternalLabel("skip_delete", skipDelete),
            )
        }
    }
}
