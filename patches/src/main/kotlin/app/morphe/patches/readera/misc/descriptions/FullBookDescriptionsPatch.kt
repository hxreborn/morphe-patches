/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.descriptions

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val fullBookDescriptionsPatch = bytecodePatch(
    name = "Full book descriptions",
    description = "Shows the whole book description in About document, including very short and very long ones.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Full book descriptions", "Shows the whole book description in About document.")
        val match = BookDescriptionFingerprint.matchAll(1..1).single()
        val (_, minimumLength, _, maximumLength) = match.instructionMatches
        val minimumRegister = (minimumLength.instruction as OneRegisterInstruction).registerA
        val maximumRegister = (maximumLength.instruction as OneRegisterInstruction).registerA
        match.method.apply {
            replaceInstruction(minimumLength.index, "const/16 v$minimumRegister, 0x0")
            replaceInstruction(maximumLength.index, "const v$maximumRegister, ${Int.MAX_VALUE}")
        }
    }
}
