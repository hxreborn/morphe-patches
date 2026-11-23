/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.scheduleddeletion

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.protonmail.misc.banner.answerAutoDeleteBannerCheck
import app.morphe.patches.protonmail.misc.banner.autoDeleteBannerStatePatch
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.protonmail.shared.uniffiCallsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.protonScheduledDeletionRowsExtensionPatch
import app.morphe.patches.shared.misc.proton.MAIL_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.forwardStub
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BANNER_CLASS = "Lapp/hxreborn/extension/protonmail/ScheduledDeletionBanner;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/ScheduledDeletion;"

private val scheduledDeletionStringsPatch = patch {
    execute {
        document("res/values/strings.xml").use { document ->
            mapOf(
                "hx_scheduled_deletion_banner" to
                    "Trash and Spam are deleted on the schedules set in hxreborn patches.",
                "hx_scheduled_deletion_banner_trash" to
                    "Trash is deleted on the schedule set in hxreborn patches.",
                "hx_scheduled_deletion_banner_spam" to
                    "Spam is deleted on the schedule set in hxreborn patches.",
            ).forEach { (name, value) ->
                val text = document.createElement("string")
                text.setAttribute("name", name)
                text.setAttribute("translatable", "false")
                text.textContent = value
                document.documentElement.appendChild(text)
            }
        }
    }
}

@Suppress("unused")
val scheduledDeletionPatch: Patch = patch(
    name = "Scheduled Trash and Spam deletion",
    description = "Deletes all messages in Trash and Spam on separate configurable schedules, " +
        "also while the app is closed. Deleted messages cannot be recovered.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(protonScheduledDeletionRowsExtensionPatch)
    dependsOn(
        patchesSettingsPatch,
        autoDeleteBannerStatePatch,
        scheduledDeletionStringsPatch,
        uniffiCallsPatch,
    )

    execute {
        markPatchApplied(MAIL_SETTINGS_CLASS, "scheduledDeletion", scheduledDeletionPatch)
        forwardStub(MAIL_SETTINGS_CLASS, "addScheduledDeletionRows", "Lapp/hxreborn/extension/protonmail/ScheduledDeletionRows;")
        answerAutoDeleteBannerCheck("isScheduled", "$BANNER_CLASS->isScheduled()Z")

        AutoDeleteBannerTextFingerprint.match().let { match ->
            val bannerTextIndex = match.instructionMatches.single().index
            val enabledIndex = match.method.indexOfFirstInstructionReversedOrThrow(
                bannerTextIndex,
                Opcode.IGET_BOOLEAN,
            )
            val enabledRegister = match.method
                .getInstruction<OneRegisterInstruction>(enabledIndex).registerA

            val bannerTextRegister = match.method
                .getInstruction<OneRegisterInstruction>(bannerTextIndex).registerA

            match.method.addInstructions(
                bannerTextIndex + 1,
                """
                    invoke-static/range { v$bannerTextRegister .. v$bannerTextRegister }, $BANNER_CLASS->textResource(I)I
                    move-result v$bannerTextRegister
                """,
            )

            match.method.addInstructions(
                enabledIndex + 1,
                """
                    invoke-static/range { v$enabledRegister .. v$enabledRegister }, $BANNER_CLASS->isAutoDeleteEnabled(Z)Z
                    move-result v$enabledRegister
                """,
            )
        }

        listOf(ScrollMessagesForLabelFingerprint, ScrollConversationsForLabelFingerprint)
            .forEach { fingerprint ->
                fingerprint.method.addInstructions(
                    0,
                    "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onMailboxShown(Ljava/lang/Object;)V",
                )
            }

        NewMailboxFingerprint.match().let { match ->
            val (newMailboxCall, mailboxResult) = match.instructionMatches
            val newMailboxInvoke = newMailboxCall.getInstruction<FiveRegisterInstruction>()
            val resultRegister = mailboxResult.getInstruction<OneRegisterInstruction>().registerA
            val sessionRegister = match.method.getFreeRegisterProvider(
                newMailboxCall.index, 1, newMailboxInvoke.registerC, newMailboxInvoke.registerD, resultRegister,
            ).getFreeRegister()
            if (sessionRegister > 15 || resultRegister > 15) {
                throw PatchException("Mailbox capture registers exceed invoke-static encoding limit")
            }

            match.method.addInstructions(
                mailboxResult.index + 1,
                "invoke-static { v$resultRegister, v$sessionRegister }, " +
                    "$EXTENSION_CLASS->captureMailbox(Ljava/lang/Object;Ljava/lang/Object;)V",
            )
            match.method.addInstructions(
                newMailboxCall.index,
                "move-object/from16 v$sessionRegister, v${newMailboxInvoke.registerC}",
            )
        }

        StartBackgroundExecutionFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onBackgroundExecution(Ljava/lang/Object;)V",
        )
    }
}
