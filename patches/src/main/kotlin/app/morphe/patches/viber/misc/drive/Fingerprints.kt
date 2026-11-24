/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.drive

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun driveErrorDialogFingerprint(dialogCode: String, messageResource: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = emptyList(),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, messageResource),
        methodCall(parameters = listOf("I"), returnType = "V", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = "Lcom/viber/voip/ui/dialogs/DialogCode;", name = dialogCode),
    ),
)

internal fun dialogMessageTextSetterFingerprint(builderClass: String) = Fingerprint(
    definingClass = builderClass,
    returnType = "V",
    parameters = listOf("Ljava/lang/CharSequence;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/lang/CharSequence;", location = MatchFirst()),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately()),
    ),
)
