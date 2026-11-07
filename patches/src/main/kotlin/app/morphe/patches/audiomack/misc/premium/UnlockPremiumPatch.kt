/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Plus features. Region-limited songs are still restricted.",
) {
    compatibleWith(AppCompatibilities.AUDIOMACK)

    execute {
        PremiumStatusFingerprint.matchSingle().method.returnEarly(true)
        val stateMatch = PremiumStateFingerprint.matchSingle()
        val (stateRead, _) = stateMatch.instructionMatches
        val stateField = stateRead.instruction.getReference<FieldReference>()!!
        premiumStatePublisherFingerprint(stateField).matchAll(2..2).forEach { match ->
            val (_, publish) = match.instructionMatches
            val premiumRegister = (publish.instruction as FiveRegisterInstruction).registerE
            match.method.addInstruction(
                publish.index,
                "sget-object v$premiumRegister, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;",
            )
        }
        val (_, _, configurationWrite) = LyricsConfigurationFingerprint.matchSingle().instructionMatches
        val configurationField = configurationWrite.instruction.getReference<FieldReference>()!!
        lyricsAvailabilityFingerprint(configurationField).matchAll(1..2).forEach { match ->
            val (_, _, configurationResult) = match.instructionMatches
            val configurationRegister = (configurationResult.instruction as OneRegisterInstruction).registerA
            match.method.addInstruction(
                configurationResult.index + 1,
                "const-string v$configurationRegister, \"true\"",
            )
        }
    }
}
