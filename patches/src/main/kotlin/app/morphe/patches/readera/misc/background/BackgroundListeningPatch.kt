/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.background

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val backgroundListeningPatch = bytecodePatch(
    name = "Background listening",
    description = "Keeps Read aloud playing after switching apps or turning the screen off.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Background listening", "Keeps Read aloud playing after switching apps or turning the screen off.")
        SpeechPingFingerprint.let {
            val pauseStart = it.instructionMatches[SPEECH_PAUSE_EVENT_MATCH].index
            val pauseReturn = it.instructionMatches[SPEECH_PAUSE_RETURN_MATCH].index
            it.method.removeInstructions(pauseStart, pauseReturn - pauseStart + 1)
        }

        readerStopFingerprint(SpeechModelFingerprint.classDef.type).instructionMatches.single()
            .getMethodCalled()
            .returnEarly()
    }
}
