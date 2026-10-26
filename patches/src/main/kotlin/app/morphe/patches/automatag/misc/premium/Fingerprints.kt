/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.automatag.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object IsProductPurchasedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("AutomaTag.db", "map_api_key_", "getSpecialSetting:"),
)

internal object IsBillingProductOwnedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/Map;", name = "get"),
        opcode(Opcode.CHECK_CAST),
        opcode(Opcode.IGET_BOOLEAN),
    ),
)
