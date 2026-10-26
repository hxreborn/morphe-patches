/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.poe.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod

val restoreDownloadReceiverPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.POE)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        DownloadCompleteReceiverFingerprint.matchSingle().classDef.apply {
            replaceVirtualizedMethod(
                "onReceive",
                5,
                """
                    const-string p1, "extra_download_id"
                    const-wide/16 v0, -0x1
                    invoke-virtual {p2, p1, v0, v1}, Landroid/content/Intent;->getLongExtra(Ljava/lang/String;J)J
                    move-result-wide p1
                    iget-object v0, p0, $type->a:Lkotlin/jvm/internal/z;
                    iget-object v0, v0, Lkotlin/jvm/internal/z;->element:Ljava/lang/Object;
                    if-eqz v0, :done
                    check-cast v0, Ljava/lang/Number;
                    invoke-virtual {v0}, Ljava/lang/Number;->longValue()J
                    move-result-wide v0
                    cmp-long p1, p1, v0
                    if-nez p1, :done
                    iget-object p1, p0, $type->b:Lcom/poe/util/k2;
                    invoke-virtual {p1}, Lcom/poe/util/k2;->invoke()Ljava/lang/Object;
                    iget-object p0, p0, $type->c:Lkotlinx/coroutines/m;
                    invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
                    move-result-object p1
                    invoke-virtual {p0, p1}, Lkotlinx/coroutines/m;->d(Ljava/lang/Object;)V
                    :done
                    return-void
                """,
            )
        }
    }
}
