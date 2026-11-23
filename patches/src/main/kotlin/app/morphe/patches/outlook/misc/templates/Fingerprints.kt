/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.templates

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val FILES_ACTION_DISPATCHER_CLASS = "Lcom/microsoft/office/outlook/file/FilesActionDispatcherImpl;"

internal object IsSupportedOftFileFingerprint : Fingerprint(
    definingClass = FILES_ACTION_DISPATCHER_CLASS,
    name = "isSupportedOftFile",
    returnType = "Z",
    filters = listOf(
        methodCall(name = "getSubscriptionStatus", parameters = emptyList()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(
            definingClass = "Lcom/microsoft/office/outlook/olmcore/enums/O365SubscriptionStatus;",
            name = "ConsumerPaid",
            opcode = Opcode.SGET_OBJECT,
        ),
        methodCall(
            definingClass = "Lcom/microsoft/office/outlook/util/FileHelper;",
            name = "isOftFile",
        ),
    ),
)

internal object OpenSupportedOftFileFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = FILES_ACTION_DISPATCHER_CLASS, name = "isSupportedOftFile"),
        instanceOf("Lcom/microsoft/office/outlook/file/FilesScenario\$ReadingPane;"),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)
