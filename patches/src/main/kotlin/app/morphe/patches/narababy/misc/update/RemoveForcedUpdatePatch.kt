/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.update

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.narababy.shared.DATABASE_SNAPSHOTS_CLASS
import app.morphe.patches.narababy.shared.rewriteDatabaseSnapshots
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.narababyDatabaseSnapshotsExtensionPatch

@Suppress("unused")
val removeForcedUpdatePatch = bytecodePatch(
    name = "Remove forced update",
    description = "Removes the update screen that blocks the app when a newer build is published.",
) {
    dependsOn(narababyDatabaseSnapshotsExtensionPatch)
    compatibleWith(AppCompatibilities.NARA_BABY)
    execute {
        rewriteDatabaseSnapshots("$DATABASE_SNAPSHOTS_CLASS->removeVersionBlock(Ljava/lang/Object;)V")
    }
}
