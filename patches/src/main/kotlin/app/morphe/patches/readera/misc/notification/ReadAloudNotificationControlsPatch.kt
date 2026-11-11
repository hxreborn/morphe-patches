/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.notification

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechNotification;"
private const val STOP_ACTION_REGISTERS = 4
private const val TIMER_READER_REGISTERS = 4
private const val STOP_ICON = 0x1080038

@Suppress("unused")
val readAloudNotificationControlsPatch = bytecodePatch(
    name = "Read aloud notification controls",
    description = "Adds Stop, Previous and Next buttons to the Read aloud notification. " +
        "On Android 7 to 10 it also counts down the sleep timer.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Read aloud notification controls", "Adds Stop, Previous and Next to the Read aloud notification.")
        val notification = SpeechNotificationFingerprint.matchAll(1..1).single()
        val (_, _, _, compactArray, build) = notification.instructionMatches
        val (_, _, stopIntent) = SpeechStopIntentFingerprint.matchAll(1..1).single().instructionMatches
        val resultIndex = build.index + 1
        check(notification.method.getInstruction(resultIndex).opcode == Opcode.MOVE_RESULT_OBJECT) {
            "No move-result-object after the speech notification build call"
        }
        val builder = build.getMethodCalled().definingClass
        val addAction = addNotificationActionFingerprint(builder).matchAll(1..1).single().method
        val addSpeechStop = mutableClassDefBy(builder).addMethod(
            "hxAddSpeechStop", listOf("Landroid/app/PendingIntent;"), "V", AccessFlags.PUBLIC.value,
            STOP_ACTION_REGISTERS,
            """
                const v0, $STOP_ICON
                const-string v1, "Stop"
                invoke-virtual { p0, v0, v1, p1 }, $addAction
                return-void
            """,
        )
        val resultRegister = notification.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
        notification.method.addInstructions(
            resultIndex + 1,
            """
                invoke-static { v$resultRegister, p0 }, $EXTENSION_CLASS_DESCRIPTOR->addSleepTimerCountdown(Landroid/app/Notification;Landroid/content/Context;)Landroid/app/Notification;
                move-result-object v$resultRegister
            """,
        )
        val builderRegister = build.getInstruction<FiveRegisterInstruction>().registerC
        val stopRegister = stopIntent.getInstruction<OneRegisterInstruction>().registerA
        notification.method.addInstructions(stopIntent.index + 1,
            "invoke-virtual { v$builderRegister, v$stopRegister }, $addSpeechStop")
        val compactRegister = compactArray.getInstruction<OneRegisterInstruction>().registerA
        notification.method.addInstructions(
            compactArray.index + 1,
            """
                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->compactActionIndices()[I
                move-result-object v$compactRegister
            """,
        )

        val stateSetter = SetPlaybackStateFingerprint.matchAll(1..1).single()
        val (setState) = stateSetter.instructionMatches
        val stateRegister = setState.getInstruction<FiveRegisterInstruction>().registerD
        stateSetter.method.addInstructions(setState.index,
            """
                invoke-static { v$stateRegister }, $EXTENSION_CLASS_DESCRIPTOR->addStopAction(Landroid/media/session/PlaybackState;)Landroid/media/session/PlaybackState;
                move-result-object v$stateRegister
            """,
        )
        val customActionCallback = CustomActionCallbackFingerprint.matchAll(1..1).single().method
        customActionCallback.addInstructions(0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->isStopAction(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :original
                invoke-virtual { p0 }, Landroid/media/session/MediaSession${'$'}Callback;->onStop()V
                return-void
                :original
                nop
            """,
        )

        val timerUpdated = SleepTimerUpdatedFingerprint.matchAll(1..1).single().method
        val refresh = RefreshSpeechNotificationFingerprint.matchAll(1..1).single().method
        val timerHandler = sleepTimerHandlerFingerprint(
            refresh.definingClass, timerUpdated.parameterTypes.single().toString(),
        ).matchAll(1..1).single().method
        timerHandler.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            timerHandler.addInstructionsAtControlFlowLabel(
                index,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->showsCountdown()Z
                    move-result v0
                    if-eqz v0, :countdown_hidden
                    invoke-direct { p0 }, $refresh
                    :countdown_hidden
                    nop
                """,
            )
        }

        val timer = SleepTimerFingerprint.matchAll(1..1).single()
        val (settings, duration, _, singleton, elapsed) = timer.instructionMatches
        val minutes = duration.instruction.getReference<FieldReference>()!!
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceMethod(
            "sleepTimerRemainingMillis", TIMER_READER_REGISTERS,
            """
                invoke-static { }, ${settings.getMethodCalled()}
                move-result-object v0
                iget v0, v0, $minutes
                invoke-static { }, ${singleton.getMethodCalled()}
                move-result-object v1
                invoke-virtual { v1 }, ${elapsed.getMethodCalled()}
                move-result-wide v1
                invoke-static { v0, v1, v2 }, $EXTENSION_CLASS_DESCRIPTOR->remainingMillisAfterElapsed(IJ)J
                move-result-wide v0
                return-wide v0
            """,
        )
    }
}
