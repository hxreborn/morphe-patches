/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.analytics

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addMethod
import com.android.tools.smali.dexlib2.AccessFlags
import app.morphe.patches.shared.misc.extension.tradingviewDisableAnalyticsExtensionPatch

private const val ANALYTICS_REQUEST_DESCRIPTOR = "Lapp/hxreborn/extension/tradingview/AnalyticsRequest;"
private const val WEB_VIEW_DESCRIPTOR = "Landroid/webkit/WebView;"
private const val WEB_RESOURCE_REQUEST_DESCRIPTOR = "Landroid/webkit/WebResourceRequest;"
private const val WEB_RESOURCE_RESPONSE_DESCRIPTOR = "Landroid/webkit/WebResourceResponse;"
private const val INTERCEPT_REQUEST_METHOD = "shouldInterceptRequest"
private const val INTERCEPT_REQUEST_REGISTER_COUNT = 4

@Suppress("unused")
val disableAnalyticsPatch = patch(
    name = "Disable analytics",
    description = "Disables network telemetry and Snowplow analytics.",
) {
    dependsOn(tradingviewDisableAnalyticsExtensionPatch)
    compatibleWith(AppCompatibilities.TRADINGVIEW)
    execute {
        NetworkTelemetryInitializationFingerprint.matchSingle().method.returnEarly()
        SnowPlowTrackerInitializationFingerprint.matchSingle().method.returnEarly()

        ChartWebViewClientFingerprint.matchSingle().classDef.apply {
            check(methods.none { it.name == INTERCEPT_REQUEST_METHOD }) {
                "WebView client already intercepts requests"
            }
            addMethod(
                INTERCEPT_REQUEST_METHOD,
                listOf(WEB_VIEW_DESCRIPTOR, WEB_RESOURCE_REQUEST_DESCRIPTOR),
                WEB_RESOURCE_RESPONSE_DESCRIPTOR,
                AccessFlags.PUBLIC.value,
                INTERCEPT_REQUEST_REGISTER_COUNT,
                """
                    invoke-static { p2 }, $ANALYTICS_REQUEST_DESCRIPTOR->intercept($WEB_RESOURCE_REQUEST_DESCRIPTOR)$WEB_RESOURCE_RESPONSE_DESCRIPTOR
                    move-result-object v0
                    return-object v0
                """,
            )
        }
    }
}
