/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes ads from the feed, app open, interstitial and rewarded placements, " +
        "and the sponsor banner on the launch screen.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        InitializeMobileAdsFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object p1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object p1
            """,
        )

        val amazon = RegisterAmazonAdsFingerprint.matchSingle()
        amazon.instructionMatches.reversed().forEach { amazon.method.removeInstruction(it.index) }

        val audienceNetwork = InitializeAudienceNetworkFingerprint.matchSingle()
        audienceNetwork.method.removeInstruction(audienceNetwork.instructionMatches.single().index)

        SplashForegroundImageFingerprint.matchSingle().apply {
            val index = instructionMatches.last().index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA
            method.addInstruction(index + 1, "const/4 v$register, 0x0")
        }
    }
}
