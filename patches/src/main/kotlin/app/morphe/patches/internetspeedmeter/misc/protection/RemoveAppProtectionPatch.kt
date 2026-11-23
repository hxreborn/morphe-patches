/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.misc.protection

import app.morphe.patcher.patch.patch
import app.morphe.patches.internetspeedmeter.misc.counters.replaceInterfaceCountersPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

internal val removeAppProtectionPatch = patch {
    compatibleWith(AppCompatibilities.INTERNET_SPEED_METER_LITE)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        replaceInterfaceCountersPatch,
    )
}
