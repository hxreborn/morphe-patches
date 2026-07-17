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
 * patches/src/main/kotlin/app/revanced/patches/projectivy/misc/tracking/Fingerprints.kt
 */
package app.morphe.patches.projectivy.misc.tracking

import app.morphe.patcher.Fingerprint

// "item_category" alone also matches a Firebase constants <clinit>; pin the class so this
// cannot silently resolve to it.
internal object AnalyticsEventFingerprint : Fingerprint(
    definingClass = "Lcom/spocky/projengmenu/utils/PTUtils;",
    strings = listOf("item_category"),
)

internal object FirebaseConfigFingerprint : Fingerprint(
    strings = listOf("Firebase Performance is Enabled"),
)

internal object AdvertisingIdClientConnectFingerprint : Fingerprint(
    strings = listOf("com.google.android.gms.ads.identifier.service.START"),
)

internal object FirebaseInstallationsUrlFingerprint : Fingerprint(
    strings = listOf("https://firebaseinstallations.googleapis.com/v1/"),
)

internal object CrashlyticsSettingsUrlFingerprint : Fingerprint(
    strings = listOf("firebase-settings.crashlytics.com"),
)
