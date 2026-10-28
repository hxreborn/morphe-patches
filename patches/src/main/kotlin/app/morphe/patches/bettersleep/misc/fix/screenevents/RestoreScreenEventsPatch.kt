/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.fix.screenevents

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags

private const val BROADCAST_RECEIVER = "Landroid/content/BroadcastReceiver;"
private const val OBJECT = "Ljava/lang/Object;"

val restoreScreenEventsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.BETTERSLEEP)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val recorder = RecordScreenEventFingerprint.matchSingle().method

        val receiverTypes = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.superclass == BROADCAST_RECEIVER && classDef.methods.any { it.parameterTypes.firstOrNull() == recorder.definingClass }) {
                receiverTypes += classDef.type
            }
        }
        val receiver = mutableClassDefBy(receiverTypes.single())

        val recorderLazyField = descriptor(receiver.instanceFields.single { it.type == OBJECT })
        val lazyType = receiver.methods.single {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == OBJECT && it.parameterTypes.size == 1
        }.parameterTypes.single().toString()

        receiver.replaceVirtualizedMethod(
            "onReceive",
            5,
            """
                invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
                move-result-object v0
                iget-object v1, p0, $recorderLazyField
                check-cast v1, $lazyType
                invoke-interface {v1}, $lazyType->getValue()$OBJECT
                move-result-object v1
                check-cast v1, ${recorder.definingClass}
                invoke-virtual {v1, v0}, ${descriptor(recorder)}
                return-void
            """,
        )
    }
}
