/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.trainline.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val ADVERT_STATE_HIDDEN =
    "Lcom/thetrainline/ads/google_ad/advert_component/AdvertState\$Hidden;"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes the adverts shown between search results.",
) {
    compatibleWith(AppCompatibilities.TRAINLINE)

    execute {
        AdvertContentFingerprint.matchSingle().method.addInstructions(
            0,
            "sget-object p3, $ADVERT_STATE_HIDDEN->a:$ADVERT_STATE_HIDDEN",
        )
    }
}
