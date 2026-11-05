/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.TERABOX_PATCH_SETTINGS_CLASS
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch

private const val PRIVILEGE_AD_FREE = 4

@Suppress("unused")
val hideAdsPatch: BytecodePatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes feed, banner and video ads, plus interstitial and app-open ads. " +
        "Rewarded ads still play when requested from Benefits.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("hideAds", hideAdsPatch)
        HasPrivilegeFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    const/4 v$register, $PRIVILEGE_AD_FREE
                    if-ne p0, v$register, :check
                    invoke-static {}, $TERABOX_PATCH_SETTINGS_CLASS->isAdsHidden()Z
                    move-result v$register
                    if-eqz v$register, :check
                    const/4 v$register, 0x1
                    return v$register
                """,
                ExternalLabel("check", getInstruction(0)),
            )
        }
    }
}
