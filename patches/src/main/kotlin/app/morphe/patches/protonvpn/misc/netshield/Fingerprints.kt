/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.netshield

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.restrictions.freeUserCheckFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

private const val LOCAL_USER_SETTINGS = "Lcom/protonvpn/android/settings/data/LocalUserSettings;"

internal fun BytecodePatchContext.netShieldAvailabilityFingerprint() = freeUserCheckFingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(vpnUserType),
)

internal fun BytecodePatchContext.netShieldResetFingerprint(): Fingerprint {
    val netShieldProtocol = classDefBy(LOCAL_USER_SETTINGS).methods.single { it.name == "getNetShield" }.returnType
    return freeUserCheckFingerprint(
        strings = listOf("reset default profile: "),
        followingFilters = arrayOf(checkCast(netShieldProtocol, location = MatchAfterWithin(8))),
    )
}
