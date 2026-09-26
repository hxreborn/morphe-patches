/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.netshield

import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patches.protonvpn.misc.restrictions.FreeUserCheckFingerprint

internal object NetShieldAvailabilityFingerprint : FreeUserCheckFingerprint(
    definingClass = "Lcom/protonvpn/android/netshield/NetShieldAvailabilityKt;",
    name = "getNetShieldAvailability",
)

internal object NetShieldResetFingerprint : FreeUserCheckFingerprint(
    "Lcom/protonvpn/android/vpn/UpdateSettingsOnVpnUserChange\$1\$1;",
    "emit\$lambda\$0",
    checkCast("Lcom/protonvpn/android/netshield/NetShieldProtocol;", location = MatchAfterWithin(8)),
)
