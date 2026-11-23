/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch: Patch = patch(
    name = "Unlock premium",
    description = "Unlocks Plus features. Region-limited songs are still restricted.",
) {
    compatibleWith(AppCompatibilities.AUDIOMACK)

    execute {
        PremiumStatusFingerprint.method.returnEarly(true)
        val stateMatch = PremiumStateFingerprint.match()
        val (stateRead, _) = stateMatch.instructionMatches
        val stateField = stateRead.fieldReference
        premiumStatePublisherFingerprint(stateField).matchAll(2..2).forEach { match ->
            val (_, publish) = match.instructionMatches
            val premiumRegister = publish.getInstruction<FiveRegisterInstruction>().registerE
            match.method.addInstruction(
                publish.index,
                "sget-object v$premiumRegister, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;",
            )
        }
        val (_, _, configurationWrite) = LyricsConfigurationFingerprint.match().instructionMatches
        val configurationField = configurationWrite.fieldReference
        lyricsAvailabilityFingerprint(configurationField).matchAll(1..2).forEach { match ->
            val (_, _, configurationResult) = match.instructionMatches
            val configurationRegister = configurationResult.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstruction(
                configurationResult.index + 1,
                "const-string v$configurationRegister, \"true\"",
            )
        }
    }
}
