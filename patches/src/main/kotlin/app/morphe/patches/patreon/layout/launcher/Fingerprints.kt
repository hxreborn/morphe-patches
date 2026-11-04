/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.launcher

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object MembershipsTabToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(string("Memberships")),
    custom = { _, classDef -> classDef.sourceFile == "LauncherV3Contract.kt" },
)

internal fun launcherTabsFingerprint(membershipsTab: String) = Fingerprint(
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = membershipsTab),
        opcode(Opcode.NEW_ARRAY),
        opcode(Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("[Ljava/lang/Object;")),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/List;", name = "contains"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "LauncherV3ViewModel.kt" },
)
