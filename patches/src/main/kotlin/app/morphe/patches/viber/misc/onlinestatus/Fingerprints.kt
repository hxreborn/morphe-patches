/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.onlinestatus

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall

private const val ONLINE_STATUS_CHANGE_LIMIT_MS = 86_400_000L

internal object OnlineStatusScreenChangeFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(name = "getLastOnlineLastChangedTime"),
        methodCall(definingClass = "Ljava/lang/Number;", name = "longValue"),
        literal(ONLINE_STATUS_CHANGE_LIMIT_MS),
    ),
)

internal object OnlineStatusPreferenceChangeFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroidx/preference/Preference;"),
    filters = listOf(
        methodCall(name = "getLastOnlineLastChangedTime"),
        methodCall(definingClass = "Ljava/lang/Long;", name = "longValue"),
        literal(ONLINE_STATUS_CHANGE_LIMIT_MS),
    ),
)
