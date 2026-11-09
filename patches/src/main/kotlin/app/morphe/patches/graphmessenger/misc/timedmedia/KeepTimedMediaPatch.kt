/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.timedmedia

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val keepTimedMediaPatch = bytecodePatch(
    name = "Keep timed media",
    description = "Stops photos, videos and voice messages with a timer from self-destructing after you open them. " +
        "Media Telegram's servers already deleted can't be restored.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        IsSecretMediaFingerprint.matchSingle().method.returnEarly(false)
        IsVoiceOnceFingerprint.matchSingle().method.returnEarly(false)
        IsRoundOnceFingerprint.matchSingle().method.returnEarly(false)

        NeedDrawBluredPreviewFingerprint.matchSingle().let { match ->
            val moveResult = match.instructionMatches.last().index
            val register = match.method.getInstruction<OneRegisterInstruction>(moveResult).registerA
            match.method.addInstruction(moveResult + 1, "return v$register")
        }

        ShouldEncryptPhotoOrVideoFingerprint.matchSingle().method.apply {
            (findInstructionIndicesReversed(mediaTimerRead) + findInstructionIndicesReversed(messageTimerRead))
                .forEach { index ->
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA
                    replaceInstruction(index, "const/4 v$register, 0x0")
                }
        }

        MarkMessageContentAsReadFingerprint.matchSingle().method.apply {
            val messageObject = parameterTypes.first()
            addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p1, $messageObject->messageOwner:$MESSAGE_CLASS
                    iget-object v0, v0, $MESSAGE_CLASS->media:$MESSAGE_MEDIA_CLASS
                    if-eqz v0, :send_read
                    iget v0, v0, $MESSAGE_MEDIA_CLASS->$MEDIA_TIMER_FIELD:I
                    if-eqz v0, :send_read
                    return-void
                """,
                ExternalLabel("send_read", getInstruction(0)),
            )
        }
    }
}
