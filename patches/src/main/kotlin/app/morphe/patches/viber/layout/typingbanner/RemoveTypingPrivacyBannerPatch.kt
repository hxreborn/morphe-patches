/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.typingbanner

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled

@Suppress("unused")
val removeTypingPrivacyBannerPatch: BytecodePatch = bytecodePatch(
    name = "Remove typing privacy banner",
    description = "Removes typing privacy promotions.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeTypingPrivacyBanner", removeTypingPrivacyBannerPatch)
        TypingPrivacyBannerEligibilityFingerprint.matchSingle().method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->removeTypingPrivacyBanner()Z",
        )
    }
}
