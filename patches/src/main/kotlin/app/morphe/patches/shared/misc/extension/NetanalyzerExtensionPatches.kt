/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val netanalyzerFeaturesExtensionPatch = patch {
    dependsOn(sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch, sharedProxyHandlerExtensionPatch, sharedPatchesMenuLayoutExtensionPatch, sharedHostedComponentsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-features.mpe")
}

internal val netanalyzerChartViewExtensionPatch = patch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-chart-view.mpe")
}

internal val netanalyzerChannelUsageActivityExtensionPatch = patch {
    dependsOn(netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-channel-usage-activity.mpe")
}

internal val netanalyzerCommonPortsListExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-common-ports-list.mpe")
}

internal val netanalyzerVisualTracerouteExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-visual-traceroute.mpe")
}

internal val netanalyzerHistoryRowsExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-history-rows.mpe")
}

internal val netanalyzerLanScanHistoryExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch, netanalyzerHistoryRowsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-lan-scan-history.mpe")
}

internal val netanalyzerToolOptionsExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-tool-options.mpe")
}

internal val netanalyzerPingStatisticsExtensionPatch = patch {
    dependsOn(netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-ping-statistics.mpe")
}

internal val netanalyzerSpeedTestExtensionPatch = patch {
    dependsOn(netanalyzerChannelUsageActivityExtensionPatch, netanalyzerChartViewExtensionPatch, netanalyzerFeaturesExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-speed-test.mpe")
}

internal val netanalyzerSettingsExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-settings.mpe")
}

internal val netanalyzerToolsHistoryExtensionPatch = patch {
    dependsOn(netanalyzerFeaturesExtensionPatch, netanalyzerHistoryRowsExtensionPatch)
    extendWith("extensions/hxreborn/netanalyzer-tools-history.mpe")
}
