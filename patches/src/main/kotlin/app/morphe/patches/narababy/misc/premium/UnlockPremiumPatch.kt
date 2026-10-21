/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.narababy.shared.DATABASE_SNAPSHOTS_CLASS
import app.morphe.patches.narababy.shared.rewriteDatabaseSnapshots
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Nara Lifetime for every family the account belongs to.",
) {
    compatibleWith(AppCompatibilities.NARA_BABY)
    extendWith("extensions/extension.mpe")

    execute {
        rewriteDatabaseSnapshots("$DATABASE_SNAPSHOTS_CLASS->addLifetimeEntitlement(Ljava/lang/Object;)V")
    }
}
