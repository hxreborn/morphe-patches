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
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.FreeServerCheckFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ServerListFilterFingerprint : FreeServerCheckFingerprint(
    parameters = listOf(
        "Z",
        "L",
        "Ljava/lang/String;",
        "L",
        "Z",
        "Ljava/lang/String;",
        "Lcom/protonvpn/android/servers/Server;",
    ),
)

internal object ServerGroupItemStateFingerprint : Fingerprint(
    parameters = listOf("L", "Ljava/lang/Integer;", "L", "L"),
    strings = listOf("filterType"),
)

internal object CountriesHeaderLabelResourceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = listOf("L", "Z"),
    filters = listOf(resourceField(ResourceType.STRING, "country_filter_all_list_header_free")),
)

internal fun BytecodePatchContext.countriesHeaderLabelFingerprint() = Fingerprint(
    filters = listOf(
        methodCall(CountriesHeaderLabelResourceFingerprint.originalMethod),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal val saveStateToStringFingerprints = listOf(
    "ServerGroupsMainScreenSaveState(selectedFilter=",
    "CitiesScreenSaveState(countryId=",
    "ServersScreenSaveState(countryId=",
).map(::ToStringFingerprint)

internal object GatewayServersSaveStateToStringFingerprint :
    ToStringFingerprint("GatewayServersScreenSaveState(gatewayName=")

internal object SearchResultSectionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf("I", "Ljava/util/List;", "L", "Ljava/lang/Integer;", "L", "Ljava/util/Locale;"),
)
