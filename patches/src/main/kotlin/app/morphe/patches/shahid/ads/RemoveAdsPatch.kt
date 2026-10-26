/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes video ads, pause ads and sponsored cards on the home, show and Shorts pages.",
) {
    compatibleWith(AppCompatibilities.SHAHID)

    execute {
        listOf(
            PlayoutAdvertisementsFingerprint,
            PlayoutNativeAdvertisementsFingerprint,
            PlayoutMediaSsaiFingerprint,
        ).forEach { fingerprint ->
            fingerprint.matchAll(2..2).forEach { it.method.returnEarly(null) }
        }

        AdLoaderLoadFingerprint.matchAll(2..2).forEach { it.method.returnEarly() }
    }
}
