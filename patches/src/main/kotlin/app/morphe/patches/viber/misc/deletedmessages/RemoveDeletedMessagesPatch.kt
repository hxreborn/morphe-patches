/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.deletedmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DELETE_SILENTLY_REGISTERS = 6
private const val SILENT_EVENT_REGISTERS = 12
private const val NOTICE_FILTER_COUNT = 3
private const val REMOVING_NOTICES = "$VIBER_SETTINGS_CLASS->removeDeletedMessageNotices()Z"
private const val STATUS_UNLESS_REMOVING = "$VIBER_SETTINGS_CLASS->hasStatusUnlessRemovingNotices(Z)Z"
private const val MIME_UNLESS_REMOVING = "$VIBER_SETTINGS_CLASS->deletedMessageMime(I)I"
private const val BODY_UNLESS_REMOVING = "$VIBER_SETTINGS_CLASS->deletedMessageBody(Ljava/lang/String;)Ljava/lang/String;"
private const val SHOW_DELETE_WITHOUT_TRACE_OPTION = "$VIBER_SETTINGS_CLASS->showDeleteWithoutTraceOption(Z)Z"

@Suppress("unused")
val removeDeletedMessagesPatch: BytecodePatch = bytecodePatch(
    name = "Remove deleted message notices",
    description = "Makes Delete for everyone leave no notice on either side and clears deleted message notices from chats. " +
        "Hides Viber's own Delete without a trace option.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeDeletedMessageNotices", removeDeletedMessagesPatch)
        val request = DeleteMessageRequestFingerprint.matchSingle()
        val (requestCall) = request.instructionMatches
        val requestRegisters = requestCall.instruction as RegisterRangeInstruction
        val silentFlag = requestRegisters.startRegister + requestRegisters.registerCount - 1
        request.method.addInstructions(
            requestCall.index,
            """
                invoke-static {}, $REMOVING_NOTICES
                move-result v$silentFlag
            """,
        )

        val silent = SilentDeletedMessageFingerprint.matchSingle()
        val (_, _, statusResult, _, statusBranch) = silent.instructionMatches
        val statusRegister = (statusResult.instruction as OneRegisterInstruction).registerA
        check((statusBranch.instruction as OneRegisterInstruction).registerA == statusRegister) {
            "Silent deletion status branch uses a different register"
        }
        val lookup = SilentMessageLookupFingerprint.matchSingle()
        check(lookup.originalMethod == silent.originalMethod) { "Silent deletion lookup method differs" }
        val (providerField, providerGet, lookupCall) = lookup.instructionMatches
        val messageQueryProvider = providerField.instruction.getReference<FieldReference>()!!
        val providerGetMethod = providerGet.instruction.getReference<MethodReference>()!!
        val findMessageByToken = lookupCall.instruction.getReference<MethodReference>()!!
        val deleted = DeletedMessageFingerprint.matchSingle()
        val (_, messageResult, _, messageUpdate, controllerRead) = deleted.instructionMatches
        val (_, bodyResult, mimeConstant) = deleted.instructionMatches.takeLast(NOTICE_FILTER_COUNT)
        val messageRegister = (messageResult.instruction as OneRegisterInstruction).registerA
        check((messageUpdate.instruction as FiveRegisterInstruction).registerD == messageRegister) {
            "Deleted message update uses a different entity register"
        }
        val event = DeletedMessageEventFingerprint.matchSingle()
        val (silentCall, normalCall) = event.instructionMatches
        check(silentCall.instruction.getReference<MethodReference>() == silent.originalMethod) {
            "Silent event handler calls a different method"
        }
        check(normalCall.instruction.getReference<MethodReference>() == deleted.originalMethod) {
            "Normal event handler calls a different method"
        }
        val bodyRegister = (bodyResult.instruction as OneRegisterInstruction).registerA
        val mimeRegister = (mimeConstant.instruction as OneRegisterInstruction).registerA
        val controller = silent.originalMethod.definingClass
        val controllerClass = mutableClassDefBy(controller)
        val silentDelete = descriptor(silent.originalMethod)
        val deleteSilently = "$controller->deleteMessageSilently($MESSAGE_ENTITY)V"
        controllerClass.addMethod(
            "deleteMessageSilently", listOf(MESSAGE_ENTITY), "V", AccessFlags.PUBLIC.value, DELETE_SILENTLY_REGISTERS, """
                invoke-static {}, $REMOVING_NOTICES
                move-result v0
                if-eqz v0, :done
                invoke-virtual {p1}, $MESSAGE_ENTITY->getMessageToken()J
                move-result-wide v0
                const/4 v2, 0x0
                invoke-virtual {p0, v0, v1, v2}, $silentDelete
                :done
                return-void
            """,
        )
        val handleEvent = "$controller->handleSilentDeletedMessage($DELETED_MESSAGE_EVENT)V"
        controllerClass.addMethod(
            "handleSilentDeletedMessage", listOf(DELETED_MESSAGE_EVENT), "V", AccessFlags.PUBLIC.value, SILENT_EVENT_REGISTERS, """
                invoke-static {}, $REMOVING_NOTICES
                move-result v0
                if-nez v0, :removing
                iget-wide v0, p1, $DELETED_MESSAGE_EVENT->messageToken:J
                const/4 v2, 0x0
                invoke-virtual {p0, v0, v1, v2}, $silentDelete
                return-void
                :removing
                iget-object v0, p0, ${descriptor(messageQueryProvider)}
                invoke-interface {v0}, ${descriptor(providerGetMethod)}
                move-result-object v0
                check-cast v0, ${findMessageByToken.definingClass}
                iget-wide v1, p1, $DELETED_MESSAGE_EVENT->messageToken:J
                invoke-virtual {v0, v1, v2}, ${descriptor(findMessageByToken)}
                move-result-object v0
                if-eqz v0, :missing
                iget-wide v0, p1, $DELETED_MESSAGE_EVENT->messageToken:J
                const/4 v2, 0x0
                invoke-virtual {p0, v0, v1, v2}, $silentDelete
                return-void
                :missing
                move-object v0, p0
                iget-object v1, p1, $DELETED_MESSAGE_EVENT->peerPhone:Ljava/lang/String;
                invoke-static {v1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
                move-result v2
                if-nez v2, :done
                const/4 v2, 0x0
                iget-wide v3, p1, $DELETED_MESSAGE_EVENT->messageToken:J
                iget-wide v5, p1, $DELETED_MESSAGE_EVENT->groupID:J
                invoke-virtual/range {v0 .. v6}, ${descriptor(deleted.originalMethod)}
                :done
                return-void
            """,
        )
        val controllerRegister = (silentCall.instruction as FiveRegisterInstruction).registerC
        event.method.replaceInstruction(silentCall.index, "invoke-virtual {v$controllerRegister, p1}, $handleEvent")
        silent.method.addInstructions(
            statusResult.index + 1,
            """
                invoke-static {v$statusRegister}, $STATUS_UNLESS_REMOVING
                move-result v$statusRegister
            """,
        )
        deleted.method.addInstructions(
            mimeConstant.index + 1,
            """
                invoke-static/range {v$mimeRegister .. v$mimeRegister}, $MIME_UNLESS_REMOVING
                move-result v$mimeRegister
            """,
        )
        deleted.method.addInstructions(
            bodyResult.index + 1,
            """
                invoke-static/range {v$bodyRegister .. v$bodyRegister}, $BODY_UNLESS_REMOVING
                move-result-object v$bodyRegister
            """,
        )
        deleted.method.addInstructions(messageUpdate.index + 1, "invoke-virtual {v${(controllerRead.instruction as TwoRegisterInstruction).registerB}, v$messageRegister}, $deleteSilently")

        DeleteWithoutTraceMenuItemFingerprint.matchSingle().apply {
            val (_, removeItem) = instructionMatches
            val keepIndex = removeItem.index + 1
            val removing = method.getFreeRegisterProvider(keepIndex, 1).getFreeRegister4Bit()
            method.addInstructionsWithLabels(
                keepIndex,
                """
                    invoke-static {}, $REMOVING_NOTICES
                    move-result v$removing
                    if-eqz v$removing, :keep
                    return-void
                """,
                ExternalLabel("keep", method.getInstruction(keepIndex)),
            )
        }
        DeleteOptionsDialogConstructorFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static {p1}, $SHOW_DELETE_WITHOUT_TRACE_OPTION
                move-result p1
            """,
        )
    }
}
