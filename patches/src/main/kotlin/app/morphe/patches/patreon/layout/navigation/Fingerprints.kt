/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal fun bottomTabToStringFingerprint(tabName: String) = Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(string(tabName)),
)

internal object NavigationBarSpecToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(string("MemberModeNavigationBarSpec")),
)

internal object NavigationBarTabsFingerprint : Fingerprint(
    classFingerprint = NavigationBarSpecToStringFingerprint,
    returnType = "L",
    parameters = listOf("Ljava/lang/String;", "Z", "Z", "Z"),
    filters = listOf(
        opcode(Opcode.NEW_ARRAY),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("[Ljava/lang/Object;")),
    ),
)

internal fun startTabRouteFingerprint(navigationBarSpec: String, feedTab: String) = Fingerprint(
    filters = listOf(
        methodCall(definingClass = navigationBarSpec, returnType = feedTab),
        methodCall(definingClass = feedTab, opcode = Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)
