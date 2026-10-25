/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.perplexity.misc.returnUnitEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val disableFirebaseSessionsAndPerformancePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_sessions_enabled", "false")
            document.putApplicationMetaData("firebase_performance_collection_enabled", "false")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops usage events, crash reports and performance data from reaching Perplexity, " +
        "Singular and Firebase.",
) {
    compatibleWith(AppCompatibilities.PERPLEXITY)

    dependsOn(
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsAndPerformancePatch,
    )

    execute {
        val payloadBuilder = AnalyticsEventPayloadFingerprint.matchSingle().method
        val dispatch = analyticsEventDispatchFingerprint(payloadBuilder).matchOrNull()

        if (dispatch == null) {
            returnUnitEarly(payloadBuilder)
        } else {
            val sinkType = dispatch.instructionMatches.last()
                .getInstruction<ReferenceInstruction>()
                .getReference<MethodReference>()!!
                .definingClass

            analyticsEventSinkFingerprint(sinkType).matchAll().forEach { returnUnitEarly(it.method) }
        }

        SingularInitFingerprint.matchSingle().method.returnEarly(false)
    }
}
