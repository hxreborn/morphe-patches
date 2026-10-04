/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.isFreeUserCall
import app.morphe.patches.protonvpn.misc.restrictions.RestrictionGuardFingerprint
import app.morphe.patches.protonvpn.misc.restrictions.freeUserCheckFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ConnectionPreferencesToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("ConnectionPreferencesState(isFeatureDiscovered="),
)

internal object ConnectionPreferencesViewStateFingerprint : Fingerprint(
    classFingerprint = ConnectionPreferencesToStringFingerprint,
    name = "<init>",
    parameters = listOf("Z", "Z", "L", "L"),
)

internal object DefaultConnectionRestrictionFingerprint : RestrictionGuardFingerprint("getDefaultProfileId")

internal object ConnectingUpdatesRecentsFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("recentsDao", "vpnStatusProvider"),
)

internal object RecentsListViewStateFlowFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("changeServerManager", "observeConnectionFeedbackViewState"),
)

internal object DefaultConnectionViewStateFlowFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("effectiveCurrentUserSettings", "getConnectIntentViewState", "observeDefaultConnection"),
)

internal object GetQuickConnectIntentConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = List(7) { "L" },
    strings = listOf("getDefaultConnectIntent", "observeDefaultConnection"),
)

internal fun BytecodePatchContext.quickConnectIntentFingerprint() = freeUserCheckFingerprint(
    definingClass = GetQuickConnectIntentConstructorFingerprint.originalClassDef.type,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
)

internal object DefaultConnectionSettingStateToStringFingerprint :
    ToStringFingerprint("DefaultConnectionSettingState(iconRes=")

internal fun BytecodePatchContext.defaultConnectionSettingFingerprint() = Fingerprint(
    name = "invokeSuspend",
    filters = listOf(
        isFreeUserCall(),
        methodCall(
            parameters = listOf("L"),
            returnType = "Ljava/lang/Long;",
            opcodes = listOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE),
        ),
        newInstance(DefaultConnectionSettingStateToStringFingerprint.originalClassDef.type),
    ),
)
