/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private val disableFirebaseSessionsPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_sessions_enabled", "false")
        }
    }
}

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops RudderStack, Branch, Firebase Sessions and Crashlytics from " +
        "collecting usage data.",
) {
    compatibleWith(AppCompatibilities.STEEZY)

    dependsOn(disableCrashlyticsCollectionPatch, disableFirebaseSessionsPatch)

    execute {
        RudderSetupFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-interface { p3, v0 }, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )

        val trackingDisabledForContext = BranchTrackingDisabledForContextFingerprint.matchSingle()
        trackingDisabledForContext.method.returnEarly(true)
        branchTrackingDisabledFingerprint(trackingDisabledForContext.classDef.type)
            .matchSingle().method.returnEarly(true)
    }
}
