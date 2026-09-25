/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val PRIVILEGE_AD_FREE = 4

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes feed, banner, interstitial, app-open, video player and rewarded ads. " +
        "Features unlocked by watching an ad are unavailable.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch)

    execute {
        AdSdkInitFingerprint.matchSingle().method.returnEarly()

        HasPrivilegeFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    const/4 v$register, $PRIVILEGE_AD_FREE
                    if-ne p0, v$register, :check
                    const/4 v$register, 0x1
                    return v$register
                """,
                ExternalLabel("check", getInstruction(0)),
            )
        }
    }
}
