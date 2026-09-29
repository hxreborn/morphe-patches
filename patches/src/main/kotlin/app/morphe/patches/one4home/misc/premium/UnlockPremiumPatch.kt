/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.one4home.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks One4Home Pro and the collector Pals.",
) {
    compatibleWith(AppCompatibilities.ONE4HOME)

    execute {
        val proBillingStateConstructor = ProBillingStateToStringFingerprint.matchSingle()
            .classDef.methods.single { it.name == "<init>" }
        val proSourceType = proBillingStateConstructor.parameterTypes[1]

        proBillingStateConstructor.addInstructions(
            0,
            """
                const/4 p1, 0x1
                const-string p2, "LIFETIME"
                invoke-static { p2 }, $proSourceType->valueOf(Ljava/lang/String;)$proSourceType
                move-result-object p2
            """,
        )

        val palAccessCheck = HomePalCatalogFingerprint.matchSingle()
            .classDef.methods.single { it.returnType == "Z" }

        palAccessCheck.returnEarly(true)
    }
}
