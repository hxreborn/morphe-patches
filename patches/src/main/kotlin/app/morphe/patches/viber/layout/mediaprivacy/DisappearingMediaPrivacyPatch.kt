/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.mediaprivacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.registersUsed
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/viber/DisappearingMediaPrivacy;"
private const val UPDATE_READ_STATE = "$EXTENSION_CLASS->updateReadState(Ljava/lang/Object;Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I"
private const val SCREENSHOT_EVENT_PARAMETER = "p5"

@Suppress("unused")
val disappearingMediaPrivacyPatch: BytecodePatch = bytecodePatch(
    name = "Disappearing media privacy",
    description = "Stops screenshot alerts and opening reports for disappearing messages. Opening reports resume once you read a newer message in that chat.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("disappearingMediaPrivacy", disappearingMediaPrivacyPatch)
        ScreenshotTakenFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disappearingMediaPrivacy()Z")
        SecretEventSendFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { $SCREENSHOT_EVENT_PARAMETER .. $SCREENSHOT_EVENT_PARAMETER }, $EXTENSION_CLASS->blockScreenshot(I)Z
                    move-result v$register
                    if-eqz v$register, :original
                    return-void
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }
        val timestampWriter = ReadTimestampWriteFingerprint.matchSingle()
        val (selectionConstant, _) = timestampWriter.instructionMatches
        val inferredUpdate = ImmutableMethod(
            EXTENSION_CLASS,
            "updateInferredReadTime",
            listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)) + timestampWriter.originalMethod.parameters,
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(timestampWriter.originalMethod.implementation!!),
        ).toMutable().apply {
            val register = getInstruction<OneRegisterInstruction>(selectionConstant.index).registerA
            addInstructions(
                selectionConstant.index + 1,
                """
                    invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->inferredReadSelection(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """,
            )
        }
        with(mutableClassDefBy(EXTENSION_CLASS)) {
            methods.remove(methods.single { it.name == inferredUpdate.name })
            methods.add(inferredUpdate)
        }
        inferredReadReceiptFingerprint(timestampWriter.originalMethod).matchSingle().apply {
            val (_, updateCall) = instructionMatches
            method.replaceInstruction(updateCall.index, staticInvoke(updateCall.instruction, descriptor(inferredUpdate)))
        }
        ReadReceiptEnqueueFingerprint.matchSingle().apply {
            val (_, updateCall) = instructionMatches
            val update = updateCall.instruction.getReference<MethodReference>()!!
            mutableClassDefBy(EXTENSION_CLASS).replaceStub(
                "update",
                update.parameterTypes.size + 1,
                """
                    check-cast p0, ${update.definingClass}
                    invoke-virtual/range { p0 .. p4 }, ${descriptor(update)}
                    move-result p0
                    return p0
                """,
            )
            method.replaceInstruction(updateCall.index, staticInvoke(updateCall.instruction, UPDATE_READ_STATE))
        }
    }
}

private fun staticInvoke(instruction: Instruction, target: String): String {
    val registers = instruction.registersUsed
    return if (instruction is RegisterRangeInstruction) {
        "invoke-static/range { v${registers.first()} .. v${registers.last()} }, $target"
    } else {
        "invoke-static { ${registers.joinToString { "v$it" }} }, $target"
    }
}
