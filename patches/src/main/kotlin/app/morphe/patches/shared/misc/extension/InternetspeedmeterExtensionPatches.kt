/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val internetspeedmeterAdPreferenceExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/internetspeedmeter-ad-preference.mpe")
}

internal val internetspeedmeterUnlockPremiumExtensionPatch = bytecodePatch {
    dependsOn(internetspeedmeterAdPreferenceExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/internetspeedmeter-unlock-premium.mpe")
}

internal val internetspeedmeterInterfaceCountersExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/internetspeedmeter-interface-counters.mpe")
}
