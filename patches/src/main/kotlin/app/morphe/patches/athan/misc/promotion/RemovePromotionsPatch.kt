/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.promotion

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removePromotionsPatch = bytecodePatch(
    name = "Remove promotions",
    description = "Removes the Athan Academy banner in the Quran tab, and the Academy popup and teacher " +
        "feedback bar in the chapter reader.",
) {
    compatibleWith(AppCompatibilities.ATHAN)

    dependsOn(spoofSignaturePatch)

    execute {
        QuranReaderBannerConfigFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        AthanAcademyBannerDismissedFingerprint.matchSingle().method.returnEarly(true)

        AthanAcademyPopupDontAskAgainFingerprint.matchSingle().method.returnEarly(true)
    }
}
