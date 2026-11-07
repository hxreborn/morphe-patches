/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.promotions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.misc.featureflags.featureFlagsPatch
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val UPGRADE_PROMOTIONS_CLASS = "${PATREON_EXTENSION_PACKAGE}UpgradePromotions;"

@Suppress("unused")
val hideUpgradePromotionsPatch: BytecodePatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Removes upgrade, rejoin and gift banners, upsell sheets, social proof and the in-feed survey.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch, featureFlagsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(UPGRADE_PROMOTIONS_CLASS, hideUpgradePromotionsPatch)

        FeedBannerCardFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $UPGRADE_PROMOTIONS_CLASS->isHidden()Z
                    move-result v0
                    if-eqz v0, :keep_banner
                    const/4 v0, 0x0
                    return-object v0
                """,
                ExternalLabel("keep_banner", getInstruction(0)),
            )
        }

        val upsellSheetState = UpsellSheetStateToStringFingerprint.classDef
        val stateInterface = upsellSheetState.interfaces.single()
        upsellStateDispatcherFingerprint(stateInterface).method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $UPGRADE_PROMOTIONS_CLASS->isHidden()Z
                    move-result v0
                    if-eqz v0, :keep_state
                    instance-of v0, p0, ${upsellSheetState.type}
                    if-eqz v0, :keep_state
                    return-void
                """,
                ExternalLabel("keep_state", getInstruction(0)),
            )
        }
    }
}
