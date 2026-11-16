/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.deletedmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
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

@Suppress("unused")
val removeDeletedMessagesPatch = bytecodePatch(
    name = "Remove deleted message notices",
    description = "Requests silent deletion and removes deleted message notices from chats.",
    default = false,
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch)

    execute {
        val request = DeleteMessageRequestFingerprint.matchSingle()
        val (requestCall) = request.instructionMatches
        val requestRegisters = requestCall.instruction as RegisterRangeInstruction
        val silentFlag = requestRegisters.startRegister + requestRegisters.registerCount - 1
        request.method.addInstructions(requestCall.index, "const/16 v$silentFlag, 0x1")

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
                invoke-virtual {p1}, $MESSAGE_ENTITY->getMessageToken()J
                move-result-wide v0
                const/4 v2, 0x0
                invoke-virtual {p0, v0, v1, v2}, $silentDelete
                return-void
            """,
        )
        val handleEvent = "$controller->handleSilentDeletedMessage($DELETED_MESSAGE_EVENT)V"
        controllerClass.addMethod(
            "handleSilentDeletedMessage", listOf(DELETED_MESSAGE_EVENT), "V", AccessFlags.PUBLIC.value, SILENT_EVENT_REGISTERS, """
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
        silent.method.addInstructions(statusResult.index + 1, "const/4 v$statusRegister, 0x0")
        deleted.method.replaceInstruction(mimeConstant.index, "const/16 v$mimeRegister, $SILENT_DELETED_MESSAGE_MIME")
        deleted.method.addInstructions(bodyResult.index + 1, "const-string v$bodyRegister, \"\"")
        deleted.method.addInstructions(messageUpdate.index + 1, "invoke-virtual {v${(controllerRead.instruction as TwoRegisterInstruction).registerB}, v$messageRegister}, $deleteSilently")
    }
}
