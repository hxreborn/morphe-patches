/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val REMOVE_HEIGHT_LIMIT =
    "Lapp/hxreborn/extension/shahid/ManifestFilter;->removeHeightLimit(Ljava/lang/String;)Ljava/lang/String;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Full HD playback. VIP titles require a subscription.",
) {
    compatibleWith(AppCompatibilities.SHAHID)
    extendWith("extensions/extension.mpe")

    execute {
        PlayoutMediaUrlFingerprint.matchAll(2..2).forEach { match ->
            val urlIndex = match.instructionMatches.first().index
            val register = match.method.getInstruction<OneRegisterInstruction>(urlIndex).registerA
            match.method.addInstructions(
                urlIndex + 1,
                """
                    invoke-static { v$register }, $REMOVE_HEIGHT_LIMIT
                    move-result-object v$register
                """,
            )
        }

        ResolutionEligibleFingerprint.matchAll(2..2).forEach { it.method.returnEarly(true) }
        SubscriptionFeatureEnabledFingerprint.matchSingle().method.returnEarly(true)
    }
}
