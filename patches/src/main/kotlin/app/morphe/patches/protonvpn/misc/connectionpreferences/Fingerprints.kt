/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.Fingerprint
import app.morphe.patches.protonvpn.misc.restrictions.RestrictionGuardFingerprint

internal object ConnectionPreferencesToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("ConnectionPreferencesState(isFeatureDiscovered="),
)

internal object ConnectionPreferencesViewStateFingerprint : Fingerprint(
    classFingerprint = ConnectionPreferencesToStringFingerprint,
    name = "<init>",
    parameters = listOf("Z", "Z", "L", "L"),
)

internal object DefaultConnectionRestrictionFingerprint : RestrictionGuardFingerprint("getDefaultProfileId")
