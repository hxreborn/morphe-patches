/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.tracking

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import app.morphe.patches.shared.misc.manifest.disableApplicationComponents
import app.morphe.patches.shared.misc.manifest.removeMetaData
import app.morphe.patches.shared.misc.manifest.removeUsesPermissions

private val trackingComponents = setOf(
    "com.king.core.ReferrerReceiver",
    "com.king.usdk.kdid.AnalyticsReceiver",
    "com.king.usdk.braze.BrazeDelayedInitializationProvider",
    "com.king.usdk.braze.pushnotification.BrazeFcmListenerService",
    "com.king.usdk.braze.pushnotification.BrazeNotificationBroadcastReceiver",
    "com.braze.push.BrazePushReceiver",
    "com.braze.BrazeFlushPushDeliveryReceiver",
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
)

private val trackingPermissions = setOf(
    "com.google.android.gms.permission.AD_ID",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
)

internal val disableTrackingManifestEntriesPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.disableApplicationComponents(trackingComponents)
            document.removeUsesPermissions(trackingPermissions)
            document.removeMetaData(setOf(
                "com.google.firebase.components:com.google.firebase.analytics.connector.internal.AnalyticsConnectorRegistrar",
                "com.appsflyer.security.uuid",
                "com.appsflyer.security.build.ts",
            ))
            document.putApplicationMetaData("APPSFLYER_ALLOW_CUSTOM_INSTALL_ID", "false")
            document.putApplicationMetaData("com.appsflyer.security.enable", "false")
            document.putApplicationMetaData("google_analytics_default_allow_ad_user_data", "false")
            document.putApplicationMetaData("google_analytics_default_allow_ad_personalization_signals", "false")
        }
    }
}
