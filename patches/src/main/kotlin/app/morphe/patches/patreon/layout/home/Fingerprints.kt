/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.home

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal fun launcherSectionItemsFingerprint(sectionSchema: String) = Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/$sectionSchema;",
    name = "getItems",
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_OBJECT)),
)

internal object LauncherFeedResponseConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/LauncherFeedQueryNetworkResponse;",
    name = "<init>",
    parameters = listOf("Ljava/util/List;", "Ljava/util/Map;", "[B", "Ljava/lang/String;", "Ljava/lang/Integer;"),
)
