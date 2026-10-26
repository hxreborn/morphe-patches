/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

val removePairipProtectionPatch = bytecodePatch {
    compatibleWith(
        AppCompatibilities.ALBO,
        AppCompatibilities.ALOA,
        AppCompatibilities.ANYTRACKER,
        AppCompatibilities.AUDIOBOOK_UNLIMITED,
        AppCompatibilities.AUDIOLAB,
        AppCompatibilities.BANGLA_CALENDAR,
        AppCompatibilities.BIOBIRD,
        AppCompatibilities.BLOCKIT,
        AppCompatibilities.BLURWALL,
        AppCompatibilities.CATZY,
        AppCompatibilities.CLINK,
        AppCompatibilities.CODDY,
        AppCompatibilities.DEPTHPIX,
        AppCompatibilities.ECHO_EQUALIZER,
        AppCompatibilities.ECHOGRAM,
        AppCompatibilities.GLASS_WIDGETS,
        AppCompatibilities.HINDU_CALENDAR,
        AppCompatibilities.INTERNET_SPEED_METER_LITE,
        AppCompatibilities.IOTOTOY,
        AppCompatibilities.JUJO,
        AppCompatibilities.KEEPA,
        AppCompatibilities.LAGGUY_WALLPAPERS,
        AppCompatibilities.LED_BLINKER,
        AppCompatibilities.LINGODEER,
        AppCompatibilities.LUMO,
        AppCompatibilities.MEDICNEET,
        AppCompatibilities.MUSICOLET,
        AppCompatibilities.MUSICOLET_CAST_PLUGIN,
        AppCompatibilities.MYMOVESET,
        AppCompatibilities.NEETPREP,
        AppCompatibilities.NOTEIN,
        AppCompatibilities.OPAL,
        AppCompatibilities.POE,
        AppCompatibilities.PRAYER_TIMES,
        AppCompatibilities.QURANIFY,
        AppCompatibilities.RATEGLANCE,
        AppCompatibilities.RUBBER_BANDS,
        AppCompatibilities.USA_VPN,
        AppCompatibilities.VLLO,
        AppCompatibilities.VPN_INDIA,
        AppCompatibilities.WALP,
        AppCompatibilities.YUKA,
    )

    execute {
        if (classDefByOrNull(LICENSE_CLIENT_CLASS) == null) return@execute

        InitializeLicenseCheckFingerprint.matchSingle().method.returnEarly()
    }
}
