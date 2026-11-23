/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.ads

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.requireArm

private const val TEEN_MODE_STRATEGY = "Lcom/transsion/ad/strategy/v;"
private const val TEEN_MODE_KEY = "mmkv_key_teen_mode"

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads. Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(ijiamiPatch)
    availability(requireArm)

    execute {
        editIjiamiPayload {
            methodWithString(TEEN_MODE_STRATEGY, TEEN_MODE_KEY).returnEarly(true)
        }
    }
}
