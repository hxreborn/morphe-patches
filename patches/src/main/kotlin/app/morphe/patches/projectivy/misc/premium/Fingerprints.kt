/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/ea27afb265862656079dea62697b42032a4704a3
 * Commit ea27afb265862656079dea62697b42032a4704a3 (2026-05-17),
 * patches/src/main/kotlin/app/revanced/patches/projectivy/misc/premium/Fingerprints.kt
 */
package app.morphe.patches.projectivy.misc.premium

import app.morphe.patcher.Fingerprint

internal object PtApplicationMarkerFingerprint : Fingerprint(
    strings = listOf("ptutils"),
)
