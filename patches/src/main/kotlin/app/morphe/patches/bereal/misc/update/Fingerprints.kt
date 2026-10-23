/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.update

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun requiredVersionGetterFingerprint(fieldName: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(name = fieldName, opcode = Opcode.IGET_OBJECT),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object InAppUpdateSettingsFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("androidInAppUpdate"),
)

internal fun inAppUpdateManagerFingerprint(settingsType: String) = Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", settingsType, "L", "L", "L"),
    filters = listOf(methodCall(definingClass = "Landroid/content/Context;", name = "getApplicationContext")),
)

internal fun inAppUpdateEnabledFingerprint(managerType: String) = Fingerprint(
    definingClass = managerType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
)
