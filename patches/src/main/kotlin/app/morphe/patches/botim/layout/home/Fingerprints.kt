/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.home

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val SEARCH_BAR_PARAMETER_INDEX = 11

internal object MainUiStateToStringFingerprint : Fingerprint(
    strings = listOf("MainUiState(noticeInfos=", ", showSearchBar="),
)

internal object MainUiStateConstructorFingerprint : Fingerprint(
    classFingerprint = MainUiStateToStringFingerprint,
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)
