/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.psiphon.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/psiphon/PlayPurchases;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all regions. Removes ads and the upgrade button.",
) {
    compatibleWith(AppCompatibilities.PSIPHON)
    extendWith("extensions/extension.mpe")

    execute {
        PlayPurchasesResponseFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p2 }, $EXTENSION_CLASS->withSyntheticSubscription(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """,
        )

        SubscriptionExpectedFingerprint.matchSingle().method.returnEarly(false)
    }
}
