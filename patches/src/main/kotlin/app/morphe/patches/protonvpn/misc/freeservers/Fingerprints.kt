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
import com.android.tools.smali.dexlib2.Opcode

private const val UI = "Lcom/protonvpn/android/redesign/countries/ui"
private const val FILTER_TYPE = "$UI/ServerFilterType;"

internal object UserInfoUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/auth/usecase/DefaultCurrentUserProvider\$1\$1\$3;",
    name = "emit",
    parameters = listOf(
        "Lcom/protonvpn/android/auth/usecase/PartialJointUserInfo;",
        "Lkotlin/coroutines/Continuation;",
    ),
    filters = listOf(methodCall(definingClass = "Lkotlinx/coroutines/flow/MutableStateFlow;", name = "setValue")),
)

internal object UserInfoInvalidateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/auth/usecase/DefaultCurrentUserProvider;",
    name = "invalidateCache",
    returnType = "V",
    parameters = emptyList(),
)

internal object ServerListFilterFingerprint : Fingerprint(
    definingClass = "$UI/ServerListViewModelDataAdapterLegacy;",
    returnType = "Z",
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/servers/Server;", name = "isFreeServer"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object ServerGroupItemStateFingerprint : Fingerprint(
    definingClass = "$UI/ServerGroupsViewModel;",
    name = "toState",
    parameters = listOf(
        "$UI/ServerGroupItemData;",
        "Ljava/lang/Integer;",
        FILTER_TYPE,
        "$UI/ServerGroupsViewModel\$ActiveConnection;",
    ),
)

internal object CountriesHeaderLabelFingerprint : Fingerprint(
    filters = listOf(methodCall(definingClass = "$UI/ServerGroupsViewModelKt;", name = "headerLabel")),
)

internal object ServerGroupsMainScreenStateFingerprint : Fingerprint(
    definingClass = "$UI/ServerGroupsMainScreenState;",
    name = "<init>",
    parameters = listOf(FILTER_TYPE, "Ljava/util/List;", "Ljava/util/List;"),
)

internal val selectedFilterFingerprints = listOf(
    "ServerGroupsMainScreenSaveState",
    "CitiesScreenSaveState",
    "ServersScreenSaveState",
    "GatewayServersScreenSaveState",
).map { state ->
    Fingerprint(
        definingClass = "$UI/$state;",
        name = "getSelectedFilter",
        returnType = FILTER_TYPE,
        parameters = emptyList(),
    )
}

internal object SearchResultSectionFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/redesign/search/ui/SearchViewModel;",
    name = "resultSection",
    returnType = "Ljava/util/List;",
)
