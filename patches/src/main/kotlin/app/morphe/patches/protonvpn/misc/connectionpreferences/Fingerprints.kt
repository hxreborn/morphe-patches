/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.Fingerprint
import app.morphe.patches.protonvpn.misc.restrictions.RestrictionGuardFingerprint

private const val STATE =
    "Lcom/protonvpn/android/redesign/settings/ui/SettingsViewModel\$SettingViewState\$ConnectionPreferencesState"

internal object ConnectionPreferencesViewStateFingerprint : Fingerprint(
    definingClass = "$STATE;",
    name = "<init>",
    parameters = listOf(
        "Z",
        "Z",
        "$STATE\$DefaultConnectionPreferences;",
        "$STATE\$ExcludedLocationsPreferences;",
    ),
)

internal object DefaultConnectionRestrictionFingerprint : RestrictionGuardFingerprint("getDefaultProfileId")
