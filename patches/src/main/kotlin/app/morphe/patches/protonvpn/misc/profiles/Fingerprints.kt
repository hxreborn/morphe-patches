/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.protonvpn.misc.restrictions.FreeUserCheckFingerprint
import com.android.tools.smali.dexlib2.Opcode

private const val PROFILES_UI = "Lcom/protonvpn/android/profiles/ui"
private const val SERVER_DATA_ADAPTER = "$PROFILES_UI/ProfilesServerDataAdapter;"

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

internal object ProfileServerFilterFingerprint : Fingerprint(
    definingClass = SERVER_DATA_ADAPTER,
    returnType = "Z",
    parameters = listOf("Lcom/protonvpn/android/servers/Server;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/servers/Server;", name = "isFreeServer"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object ProfileCitiesFingerprint : Fingerprint(
    definingClass = SERVER_DATA_ADAPTER,
    parameters = listOf(
        "Ljava/lang/String;",
        "Z",
        "Lcom/protonvpn/android/redesign/vpn/ServerFeature;",
        "Lkotlin/coroutines/Continuation;",
    ),
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/models/vpn/VpnCountry;", name = "getServerList"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object ProfilesListStateFingerprint : Fingerprint(
    definingClass = "$PROFILES_UI/ProfilesState\$ProfilesList;",
    name = "<init>",
    parameters = listOf("Ljava/util/List;"),
)
