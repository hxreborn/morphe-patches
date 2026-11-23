/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.powerampequalizer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val APPLICATION_CLASS = "Lcom/maxmpz/app/base/BaseApplication;"
private const val BUS_MESSAGE_HANDLER = "onBusMsg"

internal object LicenseResultFingerprint : Fingerprint(
    definingClass = APPLICATION_CLASS,
    name = BUS_MESSAGE_HANDLER,
    filters = listOf(
        string("res"),
        methodCall(definingClass = "Landroid/os/BaseBundle;", name = "getInt"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        string("store", MatchAfterImmediately()),
    ),
)

internal object LicenseStateReadFingerprint : Fingerprint(
    definingClass = APPLICATION_CLASS,
    name = BUS_MESSAGE_HANDLER,
    filters = listOf(
        string("store"),
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        fieldAccess(type = "I", opcode = Opcode.IGET, location = MatchAfterImmediately()),
        string("purchased"),
    ),
)

internal fun licenseStateInitFingerprint(licenseState: FieldReference) = Fingerprint(
    definingClass = licenseState.definingClass,
    name = "<clinit>",
    filters = listOf(
        fieldAccess(
            definingClass = licenseState.definingClass,
            name = licenseState.name,
            type = licenseState.type,
            opcode = Opcode.SPUT_OBJECT,
        ),
        opcode(Opcode.RETURN_VOID),
    ),
)
