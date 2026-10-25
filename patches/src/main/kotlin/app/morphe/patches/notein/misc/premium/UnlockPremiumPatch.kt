/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notein.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro features and removes the notebook limit. AI and cloud sync are not included.",
) {
    compatibleWith(AppCompatibilities.NOTEIN)
    dependsOn(removePairipProtectionPatch)

    execute {
        MembershipGetTierFingerprint.matchSingle().method.apply {
            addInstructions(
                0,
                """
                    sget-object p0, $returnType->PRO:$returnType
                    return-object p0
                """,
            )
        }
    }
}
