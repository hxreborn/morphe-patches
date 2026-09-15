/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.scheduleddeletion

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonmail.misc.banner.autoDeleteBannerStatePatch
import app.morphe.patches.protonmail.misc.settings.markPatchApplied
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BANNER_CLASS = "Lapp/hxreborn/extension/protonmail/AutoDeleteBanner;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/ScheduledDeletion;"

private val scheduledDeletionStringsPatch = resourcePatch {
    execute {
        document("res/values/strings.xml").use { document ->
            val text = document.createElement("string")
            text.setAttribute("name", "hx_scheduled_deletion_banner")
            text.setAttribute("translatable", "false")
            text.textContent =
                "Trash and Spam are deleted on the schedule set in hxreborn patches."
            document.documentElement.appendChild(text)
        }
    }
}

@Suppress("unused")
val scheduledDeletionPatch = bytecodePatch(
    name = "Scheduled Trash and Spam deletion",
    description = "Deletes all messages in Trash and Spam on a configurable schedule. " +
        "Deleted messages cannot be recovered.",
    default = false,
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(
        patchesSettingsPatch,
        resourceMappingPatch,
        autoDeleteBannerStatePatch,
        scheduledDeletionStringsPatch,
    )
    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("scheduledDeletion")

        AutoDeleteBannerTextFingerprint.matchSingle().let { match ->
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

        with(
            IoDispatcherFingerprint.matchAll(1..2)
                .map { it.classDef }
                .single { "Ljava/util/concurrent/Executor;" in it.interfaces },
        ) {
            val dispatcherInstanceField = fields.single {
                it.type == type && AccessFlags.STATIC.isSet(it.accessFlags)
            }

            mutableClassDefBy(EXTENSION_CLASS).methods
                .single { it.name == "ioDispatcher" }
                .addInstructions(
                    0,
                    """
                        sget-object v0, $type->${dispatcherInstanceField.name}:${dispatcherInstanceField.type}
                        return-object v0
                    """,
                )
        }

        listOf(ScrollMessagesForLabelFingerprint, ScrollConversationsForLabelFingerprint)
            .forEach { fingerprint ->
                fingerprint.matchSingle().method.addInstructions(
                    0,
                    "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onMailboxShown(Ljava/lang/Object;)V",
                )
            }

        NewMailboxFingerprint.matchSingle().let { match ->
            val newMailboxCall = match.instructionMatches.first()
            val call = newMailboxCall.getInstruction<FiveRegisterInstruction>()
            val result = match.instructionMatches.last()
            val resultRegister = result.getInstruction<OneRegisterInstruction>().registerA
            val sessionRegister = match.method.getFreeRegisterProvider(
                newMailboxCall.index, 1, call.registerC, call.registerD, resultRegister,
            ).getFreeRegister()
            if (sessionRegister > 15 || resultRegister > 15) {
                throw PatchException("Mailbox capture registers exceed invoke-static encoding limit")
            }

            match.method.addInstructions(
                result.index + 1,
                "invoke-static { v$resultRegister, v$sessionRegister }, " +
                    "$EXTENSION_CLASS->captureMailbox(Ljava/lang/Object;Ljava/lang/Object;)V",
            )
            match.method.addInstructions(
                newMailboxCall.index,
                "move-object/from16 v$sessionRegister, v${call.registerC}",
            )
        }

    }
}
