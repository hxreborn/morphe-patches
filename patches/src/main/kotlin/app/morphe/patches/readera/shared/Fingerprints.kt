/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object LinkCodecExecutableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/io/File;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/io/File;", name = "exists"),
        methodCall(definingClass = "Ljava/io/File;", name = "mkdirs"),
        newInstance("Lorg/readera/codec/exception/OreInstallException;"),
    ),
)

internal object TtsPrefsEventFingerprint : Fingerprint(
    strings = listOf("PrefsTtsFragment EventPrefsChanged"),
)

internal object TtsPrefsViewFingerprint : Fingerprint(
    classFingerprint = TtsPrefsEventFingerprint,
    name = "onCreateView",
    filters = listOf(
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility"),
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility", location = MatchAfterImmediately()),
    ),
)

internal fun ttsSwitchToggleFingerprint(prefsClass: String, switchField: FieldReference) = Fingerprint(
    definingClass = prefsClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.SYNTHETIC),
    returnType = "V",
    parameters = listOf(prefsClass, "Landroid/view/View;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        opcode(Opcode.XOR_INT_LIT8, MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Z"), returnType = "V", location = MatchAfterImmediately()),
        fieldAccess(switchField, Opcode.IGET_OBJECT, MatchAfterImmediately()),
    ),
)

internal fun ttsFlagSetterFingerprint(settingsClass: String, setterName: String) = Fingerprint(
    definingClass = settingsClass,
    name = setterName,
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "edit"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
    ),
)

internal fun ttsFlagInitFingerprint(settingsClass: String, flag: FieldReference) = Fingerprint(
    definingClass = settingsClass,
    name = "<init>",
    filters = listOf(
        fieldAccess(flag, Opcode.IPUT_BOOLEAN),
    ),
)

internal object DrawerMenuVisibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/view/Menu;", name = "setGroupVisible"),
        methodCall(definingClass = "Landroid/view/Menu;", name = "setGroupVisible", location = MatchAfterWithin(1)),
        methodCall(definingClass = "Landroid/view/Menu;", name = "setGroupVisible", location = MatchAfterWithin(1)),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterWithin(1)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/MenuItem;", name = "setVisible", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterWithin(1)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/MenuItem;", name = "setVisible", location = MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/Menu;", name = "findItem", location = MatchAfterImmediately()),
    ),
)
