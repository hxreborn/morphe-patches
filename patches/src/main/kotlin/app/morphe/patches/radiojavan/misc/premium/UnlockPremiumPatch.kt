/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.radiojavan.misc.premium

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.radiojavan.misc.ads.disableSubscriptionAdFlagsPatch
import app.morphe.patches.radiojavan.shared.subscriptionGetterFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks RJ Premium downloads, high quality audio, lyrics, unlimited skips and " +
        "song selection, and hides ads. Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.RADIO_JAVAN)

    dependsOn(disableSubscriptionAdFlagsPatch)

    execute {
        fun getter(name: String) = subscriptionGetterFingerprint(name).matchSingle().method

        getter("getSubscription").returnEarly("premium")
        listOf("getAllowSync", "getAllowSyncNewItems", "getAllowLyrics", "getAllowSelectPlay")
            .forEach { getter(it).returnEarly(true) }
        getter("getSkipsLimit").returnEarly(0)
    }
}
