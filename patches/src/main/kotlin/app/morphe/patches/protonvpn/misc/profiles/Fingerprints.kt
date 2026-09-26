/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.Fingerprint
import app.morphe.patches.protonvpn.misc.restrictions.FreeUserCheckFingerprint

internal object ProfileAvailabilityFingerprint : FreeUserCheckFingerprint(
    definingClass = "Lcom/protonvpn/android/profiles/ui/ProfilesViewModel;",
    name = "toItem",
)

internal object VpnCountriesFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/utils/ServerManager;",
    name = "getVpnCountries",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

internal val profileTypesFingerprints = listOf("Standard", "SecureCore", "P2P", "Gateway").map { type ->
    Fingerprint(
        definingClass = "Lcom/protonvpn/android/profiles/ui/TypeAndLocationScreenState\$$type;",
        name = "getAvailableTypes",
        returnType = "Ljava/util/List;",
        parameters = emptyList(),
    )
}
