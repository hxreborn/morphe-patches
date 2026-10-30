/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.transparentclock.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object EntitlementCacheValidFingerprint : Fingerprint(
    filters = listOf(string("entitlement_cache_ttl_days")),
)

internal object EntitlementCheckFingerprint : Fingerprint(
    classFingerprint = EntitlementCacheValidFingerprint,
    returnType = "Z",
    filters = listOf(methodCall(definingClass = "this")),
)

internal object AdFreePreferenceReadFingerprint : Fingerprint(
    filters = listOf(
        string("sp_rma"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getBoolean"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IPUT_BOOLEAN, type = "Z", location = MatchAfterImmediately()),
    ),
)

internal fun adFreeSetterFingerprint(adsManagerType: String) = Fingerprint(
    definingClass = adsManagerType,
    parameters = listOf("Landroid/content/Context;", "Z"),
)
