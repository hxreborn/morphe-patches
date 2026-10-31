/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.messages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/imo/LongTextMessages;"

private fun MutableMethod.freeRegisters(count: Int): List<Int> {
    val registerCount = implementation!!.registerCount
    val receiverRegisters = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    val parameterRegisters = receiverRegisters + parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 }
    val provider = getFreeRegisterProvider(0, count, (registerCount - parameterRegisters until registerCount).toList())
    return List(count) { provider.getFreeRegister4Bit() }
}

@Suppress("unused")
val sendLongMessagesPatch = bytecodePatch(
    name = "Send long messages",
    description = "Sends text longer than 2000 characters as consecutive messages. " +
        "Raises the chat input limit to 10000 characters. " +
        "Privacy Chat messages are cut at 2000 characters.",
) {
    compatibleWith(AppCompatibilities.IMO)

    extendWith("extensions/extension.mpe")

    execute {
        SendTextMessageFingerprint.matchSingle().method.apply {
            val (parts, imdataParts, index, partCount) = freeRegisters(4)
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->splitIfOverLimit(Ljava/lang/String;)[Ljava/lang/String;
                    move-result-object v$parts
                    if-eqz v$parts, :send_single
                    array-length v$partCount, v$parts
                    move-object/from16 v$imdataParts, p4
                    invoke-static { v$imdataParts, v$partCount }, $EXTENSION_CLASS->imdataPerPart(Lorg/json/JSONObject;I)[Lorg/json/JSONObject;
                    move-result-object v$imdataParts
                    const/4 v$index, 0x0
                    :send_part
                    if-ge v$index, v$partCount, :sent_all
                    aget-object p1, v$parts, v$index
                    aget-object p4, v$imdataParts, v$index
                    if-eqz v$index, :first_part
                    invoke-static { }, $EXTENSION_CLASS->skipNextSendInterval()V
                    :first_part
                    invoke-virtual/range { p0 .. p6 }, $definingClass->$name(${parameterTypes.joinToString("")})V
                    add-int/lit8 v$index, v$index, 0x1
                    goto :send_part
                    :sent_all
                    return-void
                """,
                ExternalLabel("send_single", getInstruction(0)),
            )
        }

        SendIntervalCheckFingerprint.matchSingle().method.apply {
            val (skipped) = freeRegisters(1)
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS->consumeSkippedSendInterval()Z
                    move-result v$skipped
                    if-eqz v$skipped, :check_interval
                    return v$skipped
                """,
                ExternalLabel("check_interval", getInstruction(0)),
            )
        }

        ChatInputLimitFingerprint.matchSingle().apply {
            val limitCall = instructionMatches[1].index
            val input = method.getInstruction<FiveRegisterInstruction>(limitCall).registerC
            method.replaceInstruction(
                limitCall,
                "invoke-static { v$input }, $EXTENSION_CLASS->raiseInputLimit(Landroid/widget/EditText;)V",
            )
        }
    }
}
