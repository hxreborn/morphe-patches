/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.background

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.returnEarly

@Suppress("unused")
val backgroundListeningPatch = bytecodePatch(
    name = "Background listening",
    description = "Keeps text-to-speech playing while the app is in the background or the screen is off.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        SpeechPingFingerprint.let {
            val pauseStart = it.instructionMatches[2].index
            it.method.removeInstructions(pauseStart, it.instructionMatches.last().index - pauseStart + 1)
        }

        readerStopFingerprint(SpeechModelFingerprint.classDef.type).instructionMatches.single()
            .getMethodCalled()
            .returnEarly()
    }
}
