/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val internetspeedmeterAdPreferenceExtensionPatch = patch {
    extendWith("extensions/hxreborn/internetspeedmeter-ad-preference.mpe")
}

internal val internetspeedmeterUnlockPremiumExtensionPatch = patch {
    dependsOn(internetspeedmeterAdPreferenceExtensionPatch, sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/internetspeedmeter-unlock-premium.mpe")
}

internal val internetspeedmeterInterfaceCountersExtensionPatch = patch {
    extendWith("extensions/hxreborn/internetspeedmeter-interface-counters.mpe")
}
