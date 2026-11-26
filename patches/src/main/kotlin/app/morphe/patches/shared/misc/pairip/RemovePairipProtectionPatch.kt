/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

val removePairipProtectionPatch: Patch = patch {
    compatibleWith(
        AppCompatibilities.ALBO,
        AppCompatibilities.ALOA,
        AppCompatibilities.ANYTRACKER,
        AppCompatibilities.ARDUINODROID,
        AppCompatibilities.AUDIOBOOK_UNLIMITED,
        AppCompatibilities.AUDIOLAB,
        AppCompatibilities.BANGLA_CALENDAR,
        AppCompatibilities.BEHISABI,
        AppCompatibilities.BIOBIRD,
        AppCompatibilities.BITTORRENT,
        AppCompatibilities.BLOCKIT,
        AppCompatibilities.BLURWALL,
        AppCompatibilities.CATZY,
        AppCompatibilities.CHESS_SCANNER,
        AppCompatibilities.CLINK,
        AppCompatibilities.CODDY,
        AppCompatibilities.CONNECTED2,
        AppCompatibilities.DEPTHPIX,
        AppCompatibilities.ECHO_EQUALIZER,
        AppCompatibilities.ECHOGRAM,
        AppCompatibilities.EXIFTOOL,
        AppCompatibilities.FILMISH,
        AppCompatibilities.FLIPACLIP,
        AppCompatibilities.FLIGHTS25,
        AppCompatibilities.GLASS_WIDGETS,
        AppCompatibilities.HABITIFY,
        AppCompatibilities.HINDU_CALENDAR,
        AppCompatibilities.HOURLY_CHIME,
        AppCompatibilities.INFINITE_PAINTER,
        AppCompatibilities.INTERNET_SPEED_METER_LITE,
        AppCompatibilities.IOTOTOY,
        AppCompatibilities.JUJO,
        AppCompatibilities.KEEPA,
        AppCompatibilities.LAGGUY_WALLPAPERS,
        AppCompatibilities.LED_BLINKER,
        AppCompatibilities.LINGODEER,
        AppCompatibilities.LUMO,
        AppCompatibilities.MANGATIME,
        AppCompatibilities.MEDICNEET,
        AppCompatibilities.MUSICOLET,
        AppCompatibilities.MUSICOLET_CAST_PLUGIN,
        AppCompatibilities.MUVIZ_WIDGETS,
        AppCompatibilities.MYMOVESET,
        AppCompatibilities.MYSCRIPT_NOTES,
        AppCompatibilities.NEETPREP,
        AppCompatibilities.NOTEIN,
        AppCompatibilities.ONE_SEC,
        AppCompatibilities.OPAL,
        AppCompatibilities.POE,
        AppCompatibilities.PRAYER_TIMES,
        AppCompatibilities.PRAYER_TIMES_WEAR,
        AppCompatibilities.QURANIFY,
        AppCompatibilities.RATEGLANCE,
        AppCompatibilities.RUBBER_BANDS,
        AppCompatibilities.STORYWISE,
        AppCompatibilities.TUNNELDOOM_VPN,
        AppCompatibilities.ULTIMATE_GUITAR,
        AppCompatibilities.USA_VPN,
        AppCompatibilities.UTORRENT,
        AppCompatibilities.VLLO,
        AppCompatibilities.VPN_INDIA,
        AppCompatibilities.WALP,
        AppCompatibilities.YUKA,
    )

    execute {
        if (classDefByOrNull(LICENSE_CLIENT_CLASS) == null) return@execute

        InitializeLicenseCheckFingerprint.method.returnEarly()
    }
}
