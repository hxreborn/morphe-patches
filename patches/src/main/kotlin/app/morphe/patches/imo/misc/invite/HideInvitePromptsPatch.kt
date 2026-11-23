/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.invite

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.imo.shared.wideRegisterPairs
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val CHATS_TABLE = "chats_new"
private const val INVITE_ENTRY_ID = "entrance.inviteFriends"

@Suppress("unused")
val hideInvitePromptsPatch = patch(
    name = "Hide invite prompts",
    description = "Hides the invite friends dialog, the invite entry in the chat list, the call sharing banner " +
        "and the Invite to Official imo dialog for unregistered numbers.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        inviteDialogCooldownFingerprint().match().apply {
            val (_, _, lastShown) = instructionMatches
            val register = lastShown.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                lastShown.index + 1,
                """
                    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
                    move-result-wide v$register
                """,
            )
        }

        inviteChatEntryFingerprint().match().apply {
            val (deleteRowsCall, _, _, entranceClosedResult) = instructionMatches
            val deleteRows = deleteRowsCall.methodReference
            val closed = entranceClosedResult.getInstruction<OneRegisterInstruction>().registerA
            val insertIndex = entranceClosedResult.index + 1
            val wideRegisters = method.wideRegisterPairs().toIntArray()
            val registers = method.getFreeRegisterProvider(insertIndex, 4, closed, *wideRegisters)
            val (table, selection, selectionArgs, notify) = List(4) { registers.getFreeRegister4Bit() }
            method.addInstructions(
                insertIndex,
                """
                    const-string v$table, "$CHATS_TABLE"
                    const-string v$selection, "buid=?"
                    const-string v$selectionArgs, "$INVITE_ENTRY_ID"
                    filled-new-array { v$selectionArgs }, [Ljava/lang/String;
                    move-result-object v$selectionArgs
                    const/4 v$notify, 0x1
                    invoke-static { v$table, v$selection, v$selectionArgs, v$notify }, $deleteRows
                    const/4 v$closed, 0x1
                """,
            )
        }

        CallShareGuideFingerprint.method.returnEarly(false)
        ContactsInviteFingerprint.method.returnEarly(false)
        DownloadFreeImoDialogFingerprint.method.returnEarly(false)
        DownloadFreeImoTipsFingerprint.method.returnEarly(false)
        DownloadFreeImoTips2Fingerprint.method.returnEarly(false)
    }
}
