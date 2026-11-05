/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.payments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object SendMoneySettingsFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("MenuSettings(showRakutenSendMoneyItem=")),
)

internal object SendMoneyConstructorFingerprint : Fingerprint(
    classFingerprint = SendMoneySettingsFingerprint,
    name = "<init>",
    parameters = listOf("Z"),
)

internal object AttachmentTypesFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("RAKUTEN_BANK"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
        string("VIBER_PAY"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun attachmentListFingerprint(bankField: FieldReference) = Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    filters = listOf(fieldAccess(reference = bankField), opcode(Opcode.RETURN_OBJECT)),
)
