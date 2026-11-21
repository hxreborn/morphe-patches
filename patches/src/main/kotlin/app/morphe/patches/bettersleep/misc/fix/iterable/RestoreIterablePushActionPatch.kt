/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.fix.iterable

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PUSH_ACTION_RECEIVER = "Lcom/iterable/iterableapi/IterablePushActionReceiver;"
private const val CONTEXT = "Landroid/content/Context;"
private const val INTENT = "Landroid/content/Intent;"
private const val ON_RECEIVE_REGISTER_COUNT = 5

private fun MethodReference.takes(vararg types: String) = parameterTypes.map(CharSequence::toString) == types.toList()

val restoreIterablePushActionPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.BETTERSLEEP)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val trampolineResume = IterableTrampolineResumeFingerprint.matchSingle().method
        val staticCalls = trampolineResume.instructions
            .filter { it.opcode == Opcode.INVOKE_STATIC }
            .mapNotNull { it.getReference<MethodReference>() }
        val closeSystemDialogs = staticCalls.single { it.takes(CONTEXT) }
        val handlePushAction = staticCalls.single { it.takes(CONTEXT, INTENT) }

        mutableClassDefBy(PUSH_ACTION_RECEIVER).replaceVirtualizedMethod(
            "onReceive",
            ON_RECEIVE_REGISTER_COUNT,
            """
                const-string v0, "requestCode"
                const/4 v1, 0x0
                invoke-virtual {p2, v0, v1}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I
                move-result v0
                const-string v1, "notification"
                invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v1
                check-cast v1, Landroid/app/NotificationManager;
                invoke-virtual {v1, v0}, Landroid/app/NotificationManager;->cancel(I)V
                invoke-static {p1}, ${descriptor(closeSystemDialogs)}
                invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
                move-result-object v0
                const-string v1, "com.iterable.push.ACTION_PUSH_ACTION"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :done
                invoke-static {p1, p2}, ${descriptor(handlePushAction)}
                :done
                return-void
            """,
        )
    }
}
