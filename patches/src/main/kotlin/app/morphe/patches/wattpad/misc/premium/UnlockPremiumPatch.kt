/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wattpad.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val PREMIUM_PLUS_TIER = 60

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium+ with unlimited offline stories, reading themes and no " +
        "interstitials in the reader. Paid stories still cost coins.",
) {
    compatibleWith(AppCompatibilities.WATTPAD)

    execute {
        val subscriptionStatusType = SubscriptionStatusToStringFingerprint.matchSingle().classDef.type
        constructorFingerprint(
            subscriptionStatusType,
            "Z", "I", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z",
        ).matchSingle().method.addInstructions(
            0,
            """
                const/16 p2, $PREMIUM_PLUS_TIER
                const-string p3, "normal"
                const/4 p6, 0x0
            """,
        )

        val headerSectionType = HeaderSectionToStringFingerprint.matchSingle().classDef.type
        constructorFingerprint(
            headerSectionType,
            "Ljava/lang/String;", "Z", "Ljava/lang/String;", "Ljava/lang/String;",
        ).matchSingle().method.addInstructions(0, "const/4 p3, 0x0")
    }
}
