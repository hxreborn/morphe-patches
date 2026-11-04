/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedBody
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods

internal const val GUARDED_ASYNC_TASK = "Lcom/facebook/react/bridge/GuardedAsyncTask;"
internal const val DO_IN_BACKGROUND = "doInBackground"
private const val DO_WORK = "doWork"
private const val ON_RECEIVE = "onReceive"

private val RESTORED_METHODS = listOf(
    RestoredMethod(
        "Lyy2;", DO_IN_BACKGROUND, 5,
        """
            const/4 v0, 0x0
            aget-object p1, p1, v0
            check-cast p1, Lty2;
            iput-object p1, p0, Lyy2;->a:Lty2;
            new-instance p1, Lvy2;
            invoke-direct {p1}, Lvy2;-><init>()V
            iput-object p1, p0, Lyy2;->c:Lvy2;
            new-instance v0, Ljava/lang/Thread;
            new-instance v1, Lz7;
            const/16 v2, 0x9
            invoke-direct {v1, p0, v2}, Lz7;-><init>(Ljava/lang/Object;I)V
            invoke-direct {v0, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
            invoke-virtual {v0}, Ljava/lang/Thread;->start()V
            return-object p1
        """,
    ),
    RestoredMethod(
        "Lj6c;", DO_IN_BACKGROUND, 5,
        """
            const/4 v0, 0x0
            aget-object p1, p1, v0
            check-cast p1, Lg6c;
            iput-object p1, p0, Lj6c;->a:Lg6c;
            new-instance p1, Lh6c;
            invoke-direct {p1}, Lh6c;-><init>()V
            iput-object p1, p0, Lj6c;->b:Lh6c;
            new-instance v0, Ljava/lang/Thread;
            new-instance v1, Liua;
            const/4 v2, 0x6
            invoke-direct {v1, p0, v2}, Liua;-><init>(Ljava/lang/Object;I)V
            invoke-direct {v0, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
            invoke-virtual {v0}, Ljava/lang/Thread;->start()V
            return-object p1
        """,
    ),
    RestoredMethod(
        "Lt47;", ON_RECEIVE, 4,
        """
            invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
            move-result-object p1
            const-string v0, "android.intent.action.CONFIGURATION_CHANGED"
            invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result p1
            if-eqz p1, :done
            iget-object p0, p0, Lt47;->a:Lcom/unistyles/NativePlatformListener;
            invoke-static {p0}, Lcom/unistyles/NativePlatformListener;->access${'$'}notifyConfigChangedWithDelay(Lcom/unistyles/NativePlatformListener;)V
            :done
            return-void
        """,
    ),
    RestoredMethod(
        "Lz30;", ON_RECEIVE, 4,
        """
            invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
            move-result-object p1
            const-string v0, "android.media.AUDIO_BECOMING_NOISY"
            invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result p1
            if-eqz p1, :done
            iget-object p1, p0, Lz30;->b:Landroid/os/Handler;
            invoke-virtual {p1, p0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
            :done
            return-void
        """,
    ),
    RestoredMethod(
        "Lcom/facebook/CustomTabMainActivity${'$'}onCreate${'$'}redirectReceiver${'$'}1;", ON_RECEIVE, 6,
        """
            iget-object p0, p0, Lcom/facebook/CustomTabMainActivity${'$'}onCreate${'$'}redirectReceiver${'$'}1;->this${'$'}0:Lcom/facebook/CustomTabMainActivity;
            new-instance p1, Landroid/content/Intent;
            const-class v0, Lcom/facebook/CustomTabMainActivity;
            invoke-direct {p1, p0, v0}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
            sget-object v0, Lcom/facebook/CustomTabMainActivity;->REFRESH_ACTION:Ljava/lang/String;
            invoke-virtual {p1, v0}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;
            sget-object v0, Lcom/facebook/CustomTabMainActivity;->EXTRA_URL:Ljava/lang/String;
            invoke-virtual {p2, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v1
            invoke-virtual {p1, v0, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
            const/high16 v0, 0x24000000
            invoke-virtual {p1, v0}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
            invoke-virtual {p0, p1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
            return-void
        """,
    ),
    RestoredMethod(
        "Lcom/facebook/CustomTabActivity${'$'}onActivityResult${'$'}closeReceiver${'$'}1;", ON_RECEIVE, 3,
        """
            iget-object p0, p0, Lcom/facebook/CustomTabActivity${'$'}onActivityResult${'$'}closeReceiver${'$'}1;->this${'$'}0:Lcom/facebook/CustomTabActivity;
            invoke-virtual {p0}, Landroid/app/Activity;->finish()V
            return-void
        """,
    ),
    RestoredMethod(
        "Landroidx/work/impl/workers/CombineContinuationsWorker;", DO_WORK, 3,
        """
            invoke-virtual {p0}, Lny5;->getInputData()Landroidx/work/a;
            move-result-object v0
            new-instance v1, Lmy5${'$'}c;
            invoke-direct {v1, v0}, Lmy5${'$'}c;-><init>(Landroidx/work/a;)V
            return-object v1
        """,
    ),
)

val restoreVirtualizedMethodsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.ULTIMATE_GUITAR)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        restoreVirtualizedMethods(RESTORED_METHODS)

        GuardedAsyncTaskDoInBackgroundFingerprint.matchSingle().method.replaceVirtualizedBody(
            """
                invoke-virtual {p0, p1}, $GUARDED_ASYNC_TASK->doInBackgroundGuarded([Ljava/lang/Object;)V
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
