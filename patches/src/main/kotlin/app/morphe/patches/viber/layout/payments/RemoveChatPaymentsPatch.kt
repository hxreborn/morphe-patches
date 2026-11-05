/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.payments

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/ChatPayments;"

@Suppress("unused")
val removeChatPaymentsPatch: BytecodePatch = bytecodePatch(
    name = "Remove chat payment actions",
    description = "Removes payment actions from chat menus.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeChatPayments", removeChatPaymentsPatch)
        SendMoneyConstructorFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->showSendMoney(Z)Z
                move-result p1
            """,
        )
        val (_, bankField) = AttachmentTypesFingerprint.matchSingle().instructionMatches
        attachmentListFingerprint(bankField.instruction.getReference<FieldReference>()!!).matchSingle().apply {
            val (_, returnList) = instructionMatches
            val register = (returnList.instruction as OneRegisterInstruction).registerA
            method.addInstructions(
                returnList.index,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->filterAttachments(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$register
                """,
            )
        }
    }
}
