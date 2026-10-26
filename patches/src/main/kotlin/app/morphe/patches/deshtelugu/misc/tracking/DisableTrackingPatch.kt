/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.deshtelugu.misc.tracking

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun Match.removeRequestEnqueue(enqueue: MethodReference) {
    val enqueueIndex = method.indexOfFirstInstructionOrThrow(instructionMatches.last().index) {
        getReference<MethodReference>() == enqueue
    }
    method.removeInstruction(enqueueIndex)
}

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops the keyboard sending typed text, voice input, usage events, device details, " +
        "install attribution and crash reports to Desh Keyboard, Meta and Firebase.",
) {
    compatibleWith(AppCompatibilities.DESH_TELUGU_KEYBOARD)

    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch, disableCrashlyticsCollectionPatch)

    execute {
        val fullSentenceSender = FullSentenceSenderFingerprint.matchSingle()
        val fullSentenceEnqueue = fullSentenceSender.instructionMatches.last()
        val enqueue = fullSentenceEnqueue.getInstruction<ReferenceInstruction>().reference as MethodReference
        fullSentenceSender.method.removeInstruction(fullSentenceEnqueue.index)

        EventBatchSenderFingerprint.matchAll().forEach { it.removeRequestEnqueue(enqueue) }
        listOf(
            InstallSenderFingerprint,
            KeyboardActiveSenderFingerprint,
            PerformanceSenderFingerprint,
            PlayStoreSearchInstallSenderFingerprint,
            QuickMessageEventSenderFingerprint,
            ContentShareEventSenderFingerprint,
            DiagnosticsSenderFingerprint,
            RawEventSenderFingerprint,
            LearnedWordsSenderFingerprint,
            VoiceInputSenderFingerprint,
            StickerStatsSenderFingerprint,
            EmojiSuggestionSenderFingerprint,
            EmojiSearchSenderFingerprint,
            ReviewPromptSenderFingerprint,
            AppSizeSenderFingerprint,
            UserHistorySenderFingerprint,
            SetupDeviceInfoSenderFingerprint,
        ).forEach { it.matchSingle().removeRequestEnqueue(enqueue) }

        KeystrokeSuggestionSenderFingerprint.matchSingle().let { match ->
            match.method.addInstruction(match.instructionMatches.last().index + 1, "return-void")
        }

        val pingerConstructor = HeartbeatPingerFingerprint.matchSingle().instructionMatches.last()
            .getInstruction<ReferenceInstruction>().reference as MethodReference
        heartbeatPingFingerprint(pingerConstructor.definingClass).matchSingle().method.returnEarly()
        MetaRetentionPingFingerprint.method.returnEarly()
    }
}
