/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val VIP_INFO = "Lcom/dubox/drive/vip/model/VipInfo;"
private const val REQUEST_COMMON_PARAMS = "Lcom/dubox/drive/kernel/architecture/net/RequestCommonParams;"
private const val REQUEST_COMMON_PARAMS_CREATOR = "Lcom/dubox/drive/kernel/architecture/net/RequestCommonParams\$RequestCommonParamsCreator;"

internal object VipInfoIsVipFingerprint : Fingerprint(
    definingClass = VIP_INFO,
    name = "isVip",
    returnType = "Z",
    parameters = emptyList(),
)

internal object VipInfoLevelFingerprint : Fingerprint(
    definingClass = VIP_INFO,
    name = "getVipLevel",
    returnType = "I",
    parameters = emptyList(),
)

internal object VipInfoIdentityFingerprint : Fingerprint(
    definingClass = VIP_INFO,
    name = "getVipIdentity",
    returnType = "I",
    parameters = emptyList(),
)

internal object NamedPrivilegeCheckFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/vip/manager/VipRightsManager;",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("vip_can_use_all_privilege"),
)

internal object CurrentVipInfoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = VIP_INFO,
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(type = VIP_INFO, opcode = Opcode.SGET_OBJECT),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object ReportedVipStatusFingerprint : Fingerprint(
    definingClass = REQUEST_COMMON_PARAMS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(methodCall(definingClass = REQUEST_COMMON_PARAMS_CREATOR, name = "isVip")),
)

internal object VipTypeQueryParameterFingerprint : Fingerprint(
    filters = listOf(
        string("vip_type"),
        methodCall(
            definingClass = REQUEST_COMMON_PARAMS,
            parameters = emptyList(),
            returnType = "Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        methodCall(name = "addQueryParameter", location = MatchAfterWithin(1)),
    ),
)

internal fun reportedVipTypeFingerprint(vipTypeGetter: MethodReference) = Fingerprint(
    definingClass = vipTypeGetter.definingClass,
    name = vipTypeGetter.name,
    returnType = vipTypeGetter.returnType,
    parameters = emptyList(),
)

internal object PlaybackSpeedFreeLimitFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/remoteconfig/PlayerSpeedMultiplierConfig;",
    name = "getFreeLimit",
    returnType = "D",
    parameters = emptyList(),
)
