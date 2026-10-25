/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.fix.screenevents

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod

val restoreScreenEventsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.BETTERSLEEP)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        mutableClassDefBy("LBo/b;").replaceVirtualizedMethod(
            "onReceive",
            5,
            """
                invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
                move-result-object v0
                const-string v1, "android.intent.action.SCREEN_ON"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :screen_off
                sget-object v0, LZe/a;->SCREEN_ON:LZe/a;
                goto :record
                :screen_off
                const-string v1, "android.intent.action.SCREEN_OFF"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :done
                sget-object v0, LZe/a;->SCREEN_OFF:LZe/a;
                :record
                iget-object v1, p0, LBo/b;->a:Ljava/lang/Object;
                check-cast v1, LVy/l;
                invoke-interface {v1}, LVy/l;->getValue()Ljava/lang/Object;
                move-result-object v1
                check-cast v1, LMe/b;
                invoke-virtual {v1, v0}, LMe/b;->c(LZe/a;)V
                :done
                return-void
            """,
        )
    }
}
