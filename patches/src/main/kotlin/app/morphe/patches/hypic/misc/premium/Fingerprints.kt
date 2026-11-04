/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val VIP_STATE_CLASS = "Lcom/lemon/lv/clipmonetize/data/VipState;"

internal object AccountVipCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/lemon/lv/clipmonetize/data/UserDetailInfo;",
            name = "isVipWithRewardAd",
        ),
    ),
)

internal object VipStateUpdateFingerprint : Fingerprint(
    strings = listOf("onVipStateChange pre:"),
    filters = listOf(
        fieldAccess(type = VIP_STATE_CLASS, opcode = Opcode.IPUT_OBJECT),
    ),
)
