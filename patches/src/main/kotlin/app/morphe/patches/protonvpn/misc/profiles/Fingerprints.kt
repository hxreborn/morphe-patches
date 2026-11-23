/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.isFreeUserCall
import app.morphe.patches.protonvpn.misc.anchors.propertyGetterCall
import app.morphe.patches.protonvpn.misc.anchors.propertyRead
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.restrictions.freeServerCheckFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun profileAvailabilityFingerprint(): Fingerprint {
    val vpnUser = vpnUserType
    return Fingerprint(
        returnType = "Ljava/lang/Object;",
        filters = listOf(
            isFreeUserCall(),
            opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        ),
        custom = { method, _ ->
            val parameters = method.parameterTypes.map { it.toString() }
            vpnUser in parameters && "Ljava/lang/Long;" in parameters && "Ljava/util/List;" in parameters
        },
    )
}

internal object ServerListsToStringFingerprint : ToStringFingerprint("ServerLists(allServers=")

internal fun vpnCountriesFingerprint() = Fingerprint(
    definingClass = "Lcom/protonvpn/android/utils/ServerManager;",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    filters = listOf(
        propertyRead(ServerListsToStringFingerprint, "vpnCountries"),
        methodCall(definingClass = "Ljava/text/Collator;", name = "getInstance"),
    ),
)

internal val profileTypesToStringFingerprints = listOf("Standard", "SecureCore", "P2P", "Gateway").map { type ->
    ToStringFingerprint("$type(availableTypes=")
}

internal fun profileServerFilterFingerprint() = freeServerCheckFingerprint(
    anyInstruction(
        opcode(Opcode.XOR_INT_LIT8),
        opcode(Opcode.XOR_INT_2ADDR),
        location = MatchAfterImmediately(),
    ),
)

internal object VpnCountryToStringFingerprint : ToStringFingerprint("VpnCountry(flag=")

internal object CityStateIdToStringFingerprint : ToStringFingerprint("CityStateId(name=")

internal fun profileCitiesOrStatesFingerprint(): Fingerprint {
    val cityStateId = CityStateIdToStringFingerprint.originalClassDef.type
    return Fingerprint(
        filters = listOf(
            propertyGetterCall(VpnCountryToStringFingerprint, "serverList"),
            opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        ),
        custom = { method, _ ->
            val parameters = method.parameterTypes.map { it.toString() }
            !AccessFlags.STATIC.isSet(method.accessFlags) && parameters.size >= 3 &&
                parameters.first() == "Ljava/lang/String;" && cityStateId !in parameters
        },
    )
}

internal object ProfilesListToStringFingerprint : ToStringFingerprint("ProfilesList(profiles=")

internal object ProfileViewItemToStringFingerprint : ToStringFingerprint("ProfileViewItem(profile=")
