/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.invite

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CHATS_TABLE = "chats_new"
private const val INVITE_ENTRY_ID = "entrance.inviteFriends"

@Suppress("unused")
val hideInvitePromptsPatch = bytecodePatch(
    name = "Hide invite prompts",
    description = "Hides the invite friends dialog, the invite entry in the chat list and the call sharing banner.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        InviteDialogCooldownFingerprint.matchSingle().method.apply {
            val keyIndex = indexOfFirstInstructionOrThrow(lastInviteTimeKey)
            val lastShownIndex = indexOfFirstInstructionOrThrow(keyIndex, Opcode.MOVE_RESULT_WIDE)
            val register = getInstruction<OneRegisterInstruction>(lastShownIndex).registerA
            addInstructions(
                lastShownIndex + 1,
                """
                    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
                    move-result-wide v$register
                """,
            )
        }

        InviteChatEntryFingerprint.matchSingle().method.apply {
            val deleteRows = getInstruction(indexOfFirstInstructionOrThrow(chatRowsDelete))
                .getReference<MethodReference>()!!
            val keyIndex = indexOfFirstInstructionOrThrow(inviteEntryClosedKey)
            val closedIndex = indexOfFirstInstructionOrThrow(keyIndex, Opcode.MOVE_RESULT)
            val closed = getInstruction<OneRegisterInstruction>(closedIndex).registerA
            val insertIndex = closedIndex + 1
            val registers = getFreeRegisterProvider(insertIndex, 4, closed)
            val (table, selection, selectionArgs, notify) = List(4) { registers.getFreeRegister4Bit() }
            addInstructions(
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

        CallShareGuideFingerprint.matchSingle().method.returnEarly(false)
    }
}
