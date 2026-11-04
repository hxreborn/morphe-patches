/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.poe.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val RECEIVER_REGISTER_COUNT = 5

val restoreDownloadReceiverPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.POE)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val receiverClass = DownloadCompleteReceiverFingerprint.matchSingle().classDef
        val (callbackCall) = DownloadCallbackFingerprint.matchAll(receiverClass, 1..1).single().instructionMatches
        val callbackMethod = callbackCall.instruction.getReference<MethodReference>()!!
        val (resumeCall) = DownloadResumeFingerprint.matchAll(receiverClass, 1..1).single().instructionMatches
        val resumeMethod = resumeCall.instruction.getReference<MethodReference>()!!

        receiverClass.apply {
            val callbackField = fields.single { it.type == callbackMethod.definingClass }
            val continuationField = fields.single { it.type == resumeMethod.definingClass }
            val downloadIdField = fields.single { it != callbackField && it != continuationField }
            val downloadIdValueField = classDefBy(downloadIdField.type).fields.single {
                it.type == "Ljava/lang/Object;"
            }

            replaceVirtualizedMethod(
                "onReceive",
                RECEIVER_REGISTER_COUNT,
                """
                    const-string p1, "extra_download_id"
                    const-wide/16 v0, -0x1
                    invoke-virtual {p2, p1, v0, v1}, Landroid/content/Intent;->getLongExtra(Ljava/lang/String;J)J
                    move-result-wide p1
                    iget-object v0, p0, ${descriptor(downloadIdField)}
                    iget-object v0, v0, ${descriptor(downloadIdValueField)}
                    if-eqz v0, :done
                    check-cast v0, Ljava/lang/Number;
                    invoke-virtual {v0}, Ljava/lang/Number;->longValue()J
                    move-result-wide v0
                    cmp-long p1, p1, v0
                    if-nez p1, :done
                    iget-object p1, p0, ${descriptor(callbackField)}
                    invoke-virtual {p1}, ${descriptor(callbackMethod)}
                    iget-object p0, p0, ${descriptor(continuationField)}
                    invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
                    move-result-object p1
                    invoke-virtual {p0, p1}, ${descriptor(resumeMethod)}
                    :done
                    return-void
                """,
            )
        }
    }
}
