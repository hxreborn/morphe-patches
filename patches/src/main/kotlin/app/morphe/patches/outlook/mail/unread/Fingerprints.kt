/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.mail.unread

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object SetMailReadStateFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/mail/actions/MailActionExecutor;",
    name = "setMailReadState",
    returnType = "V",
    filters = listOf(
        methodCall(name = "getOperation", parameters = emptyList()),
        fieldAccess(name = "MARK_READ", opcode = Opcode.SGET_OBJECT),
        fieldAccess(name = "MARK_READ_IMPLICIT", opcode = Opcode.SGET_OBJECT),
    ),
)

internal fun mailActionSourceFingerprint(mailActionClass: String) = Fingerprint(
    definingClass = mailActionClass,
    name = "getSource",
    parameters = emptyList(),
)

internal object ReadingPaneArchiveFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(name = "getImpliesMarkAsReadForReadingPane", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Lcom/microsoft/office/outlook/mailui/actions/contributions/shared/ArchiveAction;",
            name = "executeArchiveMailFlow",
        ),
    ),
)
