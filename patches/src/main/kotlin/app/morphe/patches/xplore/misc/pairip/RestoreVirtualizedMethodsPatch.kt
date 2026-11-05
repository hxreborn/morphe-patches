/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xplore.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods

private val RESTORED_METHODS = listOf(
    RestoredMethod(
        "Lwd2;", "doInBackground", 4,
        """
            invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;
            move-result-object v0
            iget-object v1, p0, Lwd2;->a:Ljava/lang/String;
            invoke-virtual {v0, v1}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V
            invoke-virtual {p0}, Lwd2;->a()V
            const-string v1, "---"
            invoke-virtual {v0, v1}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V
            const/4 v0, 0x0
            return-object v0
        """,
    ),
    RestoredMethod(
        "Lsb5;", "onReceive", 5,
        """
            if-eqz p2, :done
            invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
            move-result-object p1
            const-string v0, "android.intent.action.SCREEN_OFF"
            invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            iget-object v1, p0, Lsb5;->b:Ltb5;
            if-eqz v0, :screen_on
            const/4 p1, 0x1
            iput-boolean p1, p0, Lsb5;->a:Z
            invoke-virtual {v1}, Ltb5;->q()V
            return-void
            :screen_on
            const-string v0, "android.intent.action.SCREEN_ON"
            invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result p1
            if-eqz p1, :done
            iget-boolean p1, p0, Lsb5;->a:Z
            if-eqz p1, :reset
            invoke-virtual {v1}, Ltb5;->u()V
            :reset
            const/4 p1, 0x0
            iput-boolean p1, p0, Lsb5;->a:Z
            :done
            return-void
        """,
    ),
)

internal val restoreVirtualizedMethodsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.XPLORE)
    dependsOn(removePairipVirtualizationPatch)

    execute {
        restoreVirtualizedMethods(RESTORED_METHODS)
    }
}
