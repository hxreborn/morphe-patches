/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.apnatunnel.misc.tracking

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.apnatunnel.misc.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = resourcePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics and Firebase Sessions from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.APNA_TUNNEL_LITE)

    dependsOn(
        spoofSignaturePatch,
        disableAnalyticsCollectionPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
    )
}
