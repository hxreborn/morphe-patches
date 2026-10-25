/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.fix.iterable

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.smali.descriptor

val restoreIterableRequestsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.BETTERSLEEP)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val executeRequest = ExecuteIterableRequestFingerprint.matchSingle().method
        val requestTask = mutableClassDefBy(executeRequest.definingClass)
        val requestType = executeRequest.parameterTypes.single().toString()
        val requestField = descriptor(requestTask.instanceFields.single { it.type == requestType })

        requestTask.methods.single { it.name == "doInBackground" && it.returnType == "Ljava/lang/Object;" }
            .addInstructionsWithLabels(
                0,
                """
                    if-eqz p1, :execute
                    array-length v0, p1
                    if-eqz v0, :execute
                    const/4 v0, 0x0
                    aget-object v0, p1, v0
                    check-cast v0, $requestType
                    iput-object v0, p0, $requestField
                    :execute
                    iget-object v0, p0, $requestField
                    invoke-static {v0}, ${descriptor(executeRequest)}
                    move-result-object v0
                    return-object v0
                """,
            )
    }
}
