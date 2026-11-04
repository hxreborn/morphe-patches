/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.featureflags

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE

private const val FEATURE_FLAGS_CLASS = "${PATREON_EXTENSION_PACKAGE}FeatureFlags;"

internal val featureFlagsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        listOf(UserFlagCheckFingerprint, CampaignFlagCheckFingerprint).forEach { fingerprint ->
            fingerprint.method.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static { p1 }, $FEATURE_FLAGS_CLASS->isForcedOff(Ljava/lang/Enum;)Z
                        move-result v0
                        if-eqz v0, :keep_flag
                        const/4 v0, 0x0
                        return v0
                    """,
                    ExternalLabel("keep_flag", getInstruction(0)),
                )
            }
        }
    }
}
