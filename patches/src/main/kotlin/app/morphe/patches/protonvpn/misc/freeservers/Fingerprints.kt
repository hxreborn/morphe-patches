/*
 * Copyright (C) 2026 Rushi Ranpise
 * Copyright (C) 2026 Paresh Maheshwari
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from rushiranpise/morphe-patches:
 * https://github.com/rushiranpise/morphe-patches/commit/81207e12513860720ac4f56c90d582a0c7e008b6
 * Commit 81207e12513860720ac4f56c90d582a0c7e008b6 (2026-09-15),
 * patches/src/main/kotlin/app/template/patches/protonvpn/premium/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.freeservers

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.freeServerCheckFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method

internal fun serverListFilterFingerprint() = freeServerCheckFingerprint(
    opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
)

internal object ServerGroupToStringFingerprint : ToStringFingerprint("ServerGroup(data=")

internal fun Method.tierParameterIndex() = parameterTypes.indexOfFirst { it.toString() == "Ljava/lang/Integer;" }

internal fun serverGroupItemStateFingerprint(): Fingerprint {
    val serverGroup = ServerGroupToStringFingerprint.originalClassDef.type
    return Fingerprint(
        returnType = serverGroup,
        filters = listOf(newInstance(serverGroup)),
        custom = { method, _ -> method.tierParameterIndex() > 0 },
    )
}

internal object CountriesHeaderLabelFingerprint : Fingerprint(
    filters = listOf(resourceField(ResourceType.STRING, "country_filter_all_list_header_free")),
)

internal val saveStateToStringFingerprints = listOf(
    "ServerGroupsMainScreenSaveState(selectedFilter=",
    "CitiesScreenSaveState(countryId=",
    "ServersScreenSaveState(countryId=",
).map(::ToStringFingerprint)

internal fun searchResultSectionFingerprint() = Fingerprint(
    filters = listOf(methodCall(serverGroupItemStateFingerprint().originalMethod)),
    custom = { method, _ ->
        val parameters = method.parameterTypes.map { it.toString() }
            .let { if (AccessFlags.STATIC.isSet(method.accessFlags)) it.drop(1) else it }
        !AccessFlags.SYNTHETIC.isSet(method.accessFlags) && parameters.size == 6 && parameters[0] == "I" && parameters[1] == "Ljava/util/List;" &&
            parameters[3] == "Ljava/lang/Integer;" && parameters[5] == "Ljava/util/Locale;"
    },
)
