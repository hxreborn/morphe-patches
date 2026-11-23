/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.mail.unread

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val READING_PANE_CLOSE_SOURCE = "READING_PANE_CLOSE"

@Suppress("unused")
val keepMessagesUnreadPatch: Patch = patch(
    name = "Keep messages unread",
    description = "Leaves messages unread after opening them, or flagging or moving them from the reading pane.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("keepMessagesUnread", keepMessagesUnreadPatch)
        val setMailReadState = SetMailReadStateFingerprint.match()
        val (operationCall, markReadAccess, markReadImplicitAccess) = setMailReadState.instructionMatches
        val operationGetter = operationCall.getInstruction<ReferenceInstruction>().reference
        val markRead = markReadAccess.getInstruction<ReferenceInstruction>().reference
        val markReadImplicit = markReadImplicitAccess.getInstruction<ReferenceInstruction>().reference

        setMailReadState.method.apply {
            val sourceGetter = mailActionSourceFingerprint(parameterTypes.single().toString()).originalMethod
            val readingPaneClose = classDefBy(sourceGetter.returnType).fields
                .singleOrNull { it.name == READING_PANE_CLOSE_SOURCE }
                ?: throw PatchException("${sourceGetter.returnType} has no $READING_PANE_CLOSE_SOURCE constant")
            val registers = getFreeRegisterProvider(0, 2)
            val actionPropertyRegister = registers.getFreeRegister4Bit()
            val enumConstantRegister = registers.getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->keepMessagesUnread()Z
                    move-result v$actionPropertyRegister
                    if-eqz v$actionPropertyRegister, :set_read_state
                    invoke-virtual {p1}, $operationGetter
                    move-result-object v$actionPropertyRegister
                    sget-object v$enumConstantRegister, $markReadImplicit
                    if-eq v$actionPropertyRegister, v$enumConstantRegister, :keep_unread
                    sget-object v$enumConstantRegister, $markRead
                    if-ne v$actionPropertyRegister, v$enumConstantRegister, :set_read_state
                    invoke-virtual {p1}, $sourceGetter
                    move-result-object v$actionPropertyRegister
                    sget-object v$enumConstantRegister, $readingPaneClose
                    if-ne v$actionPropertyRegister, v$enumConstantRegister, :set_read_state
                    :keep_unread
                    return-void
                """,
                ExternalLabel("set_read_state", getInstruction(0)),
            )
        }

        val readingPaneArchive = ReadingPaneArchiveFingerprint.match()
        val (_, impliesMarkAsReadResult) = readingPaneArchive.instructionMatches
        readingPaneArchive.method.apply {
            val impliesMarkAsReadRegister = impliesMarkAsReadResult.getInstruction<OneRegisterInstruction>().registerA
            val afterResultIndex = impliesMarkAsReadResult.index + 1
            addInstructionsWithLabels(
                afterResultIndex,
                """
                    if-eqz v$impliesMarkAsReadRegister, :archive
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->keepMessagesUnread()Z
                    move-result v$impliesMarkAsReadRegister
                    xor-int/lit8 v$impliesMarkAsReadRegister, v$impliesMarkAsReadRegister, 0x1
                """,
                ExternalLabel("archive", getInstruction(afterResultIndex)),
            )
        }
    }
}
