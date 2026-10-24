/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import app.morphe.patches.shared.misc.pairip.virtualizedMethods

private const val ACTION_CALLBACK_RECEIVER = "Landroidx/glance/appwidget/action/ActionCallbackBroadcastReceiver;"

val restoreWidgetActionsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.LUMO)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val receiver = mutableClassDefBy(ACTION_CALLBACK_RECEIVER)
        if (receiver.virtualizedMethods().isEmpty()) return@execute

        receiver.replaceVirtualizedMethod(
            "onReceive",
            8,
            """
                const-string v0, "ActionCallbackBroadcastReceiver:parameters"
                invoke-virtual {p2, v0}, Landroid/content/Intent;->getBundleExtra(Ljava/lang/String;)Landroid/os/Bundle;
                move-result-object v0
                new-instance v1, Ljava/util/LinkedHashMap;
                invoke-direct {v1}, Ljava/util/LinkedHashMap;-><init>()V
                if-eqz v0, :run
                invoke-virtual {v0}, Landroid/os/BaseBundle;->keySet()Ljava/util/Set;
                move-result-object v2
                invoke-interface {v2}, Ljava/util/Set;->iterator()Ljava/util/Iterator;
                move-result-object v2
                :next
                invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z
                move-result v3
                if-eqz v3, :run
                invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v3
                check-cast v3, Ljava/lang/String;
                invoke-virtual {v0, v3}, Landroid/os/BaseBundle;->get(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v4
                new-instance p0, Lh9;
                invoke-direct {p0, v3}, Lh9;-><init>(Ljava/lang/String;)V
                invoke-virtual {v1, p0, v4}, Ljava/util/LinkedHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                goto :next
                :run
                const-string v0, "ActionCallbackBroadcastReceiver:callbackClass"
                invoke-virtual {p2, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :done
                invoke-static {v0}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
                move-result-object v0
                invoke-virtual {v0}, Ljava/lang/Class;->newInstance()Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Lv8;
                new-instance v2, Lur3;
                invoke-direct {v2, v1}, Lur3;-><init>(Ljava/util/LinkedHashMap;)V
                invoke-interface {v0, p1, v2}, Lv8;->a(Landroid/content/Context;Lur3;)Leo6;
                :done
                return-void
            """,
        )
    }
}
