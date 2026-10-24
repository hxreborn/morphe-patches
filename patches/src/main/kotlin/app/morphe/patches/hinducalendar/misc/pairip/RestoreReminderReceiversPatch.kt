/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hinducalendar.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod

private const val APP_PACKAGE = "Lcom/alokm/hinducalendar"
private const val ON_RECEIVE = "onReceive"

val restoreReminderReceiversPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.HINDU_CALENDAR)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        mutableClassDefBy("$APP_PACKAGE/EventNotifReceiver;").replaceVirtualizedMethod(
            ON_RECEIVE,
            7,
            """
                invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
                move-result-object v0
                const-string v1, "com.alokm.hinducalendar.EVENT_NOTIFY_TICK"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :done
                invoke-virtual {p0}, Landroid/content/BroadcastReceiver;->goAsync()Landroid/content/BroadcastReceiver${'$'}PendingResult;
                move-result-object v0
                invoke-virtual {p1}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
                move-result-object v1
                new-instance v2, Lgb;
                const/4 v3, 0x7
                invoke-direct {v2, v1, v0, v3}, Lgb;-><init>(Ljava/lang/Object;Ljava/lang/Object;B)V
                new-instance v0, Ljava/lang/Thread;
                invoke-direct {v0, v2}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                :done
                return-void
            """,
        )
        mutableClassDefBy("$APP_PACKAGE/BootReceiver;").replaceVirtualizedMethod(
            ON_RECEIVE,
            5,
            """
                invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
                move-result-object v0
                const-string v1, "android.intent.action.BOOT_COMPLETED"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :done
                invoke-static {p1}, Law0;->K(Landroid/content/Context;)V
                :done
                return-void
            """,
        )
    }
}
