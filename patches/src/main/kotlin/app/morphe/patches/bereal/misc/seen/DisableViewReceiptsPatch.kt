/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.seen

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}seen/ViewReceipts;"

private fun Match.returnSuccessWhenSkipped() {
    val success = instructionMatch(Opcode.NEW_INSTANCE)
        .getInstruction<ReferenceInstruction>().getReference<TypeReference>()!!.type
    val unit = instructionMatch(Opcode.SGET_OBJECT)
        .getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!
    val registers = method.getFreeRegisterProvider(0, 2)
    val result = registers.getFreeRegister4Bit()
    val value = registers.getFreeRegister4Bit()
    method.addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $EXTENSION_CLASS->skipped()Z
            move-result v$result
            if-eqz v$result, :continue
            new-instance v$result, $success
            sget-object v$value, ${unit.definingClass}->${unit.name}:${unit.type}
            invoke-direct { v$result, v$value }, $success-><init>(Ljava/lang/Object;)V
            return-object v$result
        """,
        ExternalLabel("continue", method.getInstruction(0)),
    )
}

@Suppress("unused")
val disableViewReceiptsPatch = bytecodePatch(
    name = "Disable view receipts",
    description = "Stops sending which BeReals you viewed and which chats you opened.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val requestType = MarkPostsAsSeenRpcFingerprint.matchSingle().method.parameterTypes.first().toString()
        markPostsAsSeenRepositoryFingerprint(requestType).matchSingle().returnSuccessWhenSkipped()

        val operationType = EnterChatOperationFingerprint.matchSingle().classDef.type
        enterChatRepositoryFingerprint(operationType).matchSingle().returnSuccessWhenSkipped()
    }
}
