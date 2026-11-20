/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val netanalyzerFeaturesExtensionPatch = bytecodePatch {
    dependsOn(sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch, sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-features.mpe")
}

internal val netanalyzerChartViewExtensionPatch = bytecodePatch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-chart-view.mpe")
}

internal val netanalyzerChannelUsageActivityExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-channel-usage-activity.mpe")
}

internal val netanalyzerCommonPortsListExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-common-ports-list.mpe")
}

internal val netanalyzerVisualTracerouteExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-visual-traceroute.mpe")
}

internal val netanalyzerHistoryRowsExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-history-rows.mpe")
}

internal val netanalyzerLanScanHistoryExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch, netanalyzerHistoryRowsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-lan-scan-history.mpe")
}

internal val netanalyzerToolOptionsExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-tool-options.mpe")
}

internal val netanalyzerPingStatisticsExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-ping-statistics.mpe")
}

internal val netanalyzerSpeedTestExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerChannelUsageActivityExtensionPatch, netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-speed-test.mpe")
}

internal val netanalyzerSettingsExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-settings.mpe")
}

internal val netanalyzerToolsHistoryExtensionPatch = bytecodePatch {
    dependsOn(netanalyzerFeaturesExtensionPatch, netanalyzerHistoryRowsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-tools-history.mpe")
}
