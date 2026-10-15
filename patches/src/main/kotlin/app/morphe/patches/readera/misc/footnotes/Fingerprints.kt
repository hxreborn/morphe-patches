/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.footnotes

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference


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

internal object FootnoteUtteranceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/List;", name = "get"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, MatchAfterImmediately()),
        opcode(Opcode.IGET, MatchAfterImmediately()),
        literal(0x16, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, MatchAfterImmediately()),
    ),
)

internal fun skipFootnoteFingerprint(utteranceClass: String, isFootnote: String) = Fingerprint(
    definingClass = utteranceClass,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = utteranceClass, name = isFootnote),
    ),
)

internal object SpeakFootnoteFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("ReadTextPlayer speak note num:[%s], text:[%s]"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_DIRECT, returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_DIRECT, returnType = "Ljava/lang/String;", location = MatchAfterImmediately()),
        opcode(Opcode.SGET_BOOLEAN, MatchAfterImmediately()),
    ),
)
