/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.one4home.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val LIFETIME_SOURCE = "LIFETIME"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks One4Home Pro.",
) {
    compatibleWith(AppCompatibilities.ONE4HOME)

    execute {
        ProBillingStateToStringFingerprint.matchSingle().classDef.methods
            .single { it.name == "<init>" }
            .apply {
                val sourceType = parameterTypes[1].toString()

                addInstructions(
                    0,
                    """
                        const/4 p1, 0x1
                        const-string p2, "$LIFETIME_SOURCE"
                        invoke-static { p2 }, $sourceType->valueOf(Ljava/lang/String;)$sourceType
                        move-result-object p2
                    """,
                )
            }
    }
}
