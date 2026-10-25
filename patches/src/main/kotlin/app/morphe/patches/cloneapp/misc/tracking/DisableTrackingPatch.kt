/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val UPLOAD_ENDPOINTS = listOf("evt", "clonelog_gp", "shis", "dhis")

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops sending usage events, the cloned app list and the private browser's search and " +
        "download history to the developer, and disables Firebase Analytics.",
) {
    compatibleWith(AppCompatibilities.CLONE_APP)

    dependsOn(disableAnalyticsCollectionPatch, deactivateFirebaseAnalyticsPatch)

    execute {
        UPLOAD_ENDPOINTS.forEach { endpoint ->
            serverUploadFingerprint(endpoint).matchSingle().apply {
                val newRequest = instructionMatches.last()
                val requestType = newRequest.getInstruction<ReferenceInstruction>()
                    .getReference<MethodReference>()!!.returnType
                val send = method.indexOfFirstInstructionOrThrow(
                    newRequest.index + 1,
                    methodCall(definingClass = requestType, returnType = "V", opcode = Opcode.INVOKE_VIRTUAL),
                )

                method.removeInstruction(send)
            }
        }
    }
}
