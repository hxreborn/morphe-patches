/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xplore.misc.pairip

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods

private val RESTORED_METHODS = listOf(
    RestoredMethod(
        "Lgg2;", "doInBackground", 4,
        """
            invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;
            move-result-object v0
            iget-object v1, p0, Lgg2;->a:Ljava/lang/String;
            invoke-virtual {v0, v1}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V
            invoke-virtual {p0}, Lgg2;->a()V
            const-string v1, "---"
            invoke-virtual {v0, v1}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V
            const/4 v0, 0x0
            return-object v0
        """,
    ),
)

internal val restoreVirtualizedMethodsPatch = patch {
    compatibleWith(AppCompatibilities.XPLORE)
    dependsOn(removePairipVirtualizationPatch)

    execute {
        restoreVirtualizedMethods(RESTORED_METHODS)
    }
}
