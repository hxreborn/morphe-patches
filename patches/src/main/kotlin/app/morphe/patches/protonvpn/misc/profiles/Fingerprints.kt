/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.findPropertyGetter
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.restrictions.FreeServerCheckFingerprint
import app.morphe.patches.protonvpn.misc.restrictions.freeUserCheckFingerprint
import com.android.tools.smali.dexlib2.Opcode

private const val SERVER = "Lcom/protonvpn/android/servers/Server;"

internal fun BytecodePatchContext.profileAvailabilityFingerprint() = freeUserCheckFingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "L",
        vpnUserType,
        "Ljava/lang/Long;",
        "Lcom/protonvpn/android/vpn/ProtocolSelection;",
        "Ljava/util/List;",
        "L",
    ),
)

internal object ServerListsToStringFingerprint : ToStringFingerprint("ServerLists(allServers=")

internal fun BytecodePatchContext.vpnCountriesFingerprint() = Fingerprint(
    definingClass = "Lcom/protonvpn/android/utils/ServerManager;",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    filters = listOf(
        methodCall(findPropertyGetter(ServerListsToStringFingerprint, "vpnCountries")),
        methodCall(definingClass = "Ljava/text/Collator;", name = "getInstance"),
    ),
)

internal val profileTypesToStringFingerprints = listOf("Standard", "SecureCore", "P2P", "Gateway").map { type ->
    ToStringFingerprint("$type(availableTypes=")
}

internal object ProfileServerFilterFingerprint : FreeServerCheckFingerprint(parameters = listOf(SERVER))

internal object ProfileCitiesOrStatesFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "Z", "L", "L"),
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/models/vpn/VpnCountry;", name = "getServerList"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object ProfilesListToStringFingerprint : ToStringFingerprint("ProfilesList(profiles=")

internal object ProfilesListStateFingerprint : Fingerprint(
    classFingerprint = ProfilesListToStringFingerprint,
    name = "<init>",
    parameters = listOf("Ljava/util/List;"),
)

internal object ProfileViewItemToStringFingerprint : ToStringFingerprint("ProfileViewItem(profile=")
