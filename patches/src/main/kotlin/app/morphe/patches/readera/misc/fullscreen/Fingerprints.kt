/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.fullscreen

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PREFS_FRAGMENT_CLASS = "Lorg/readera/pref/PrefsFragment;"

internal object SaveFullScreenModeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "edit"),
        opcode(Opcode.SGET_OBJECT),
        opcode(Opcode.SGET_OBJECT),
        methodCall(definingClass = "Landroid/content/SharedPreferences\$Editor;", name = "apply"),
    ),
)

internal object FullScreenPreferencesFingerprint : Fingerprint(
    definingClass = PREFS_FRAGMENT_CLASS,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.CONST_4),
        methodCall(
            definingClass = "Landroidx/preference/Preference;",
            parameters = listOf("Z"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.CONST_4),
        methodCall(
            definingClass = "Landroidx/preference/Preference;",
            parameters = listOf("Z"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = PREFS_FRAGMENT_CLASS, opcode = Opcode.INVOKE_DIRECT, location = MatchAfterImmediately()),
    ),
)

internal object RootPreferencesFingerprint : Fingerprint(
    definingClass = PREFS_FRAGMENT_CLASS,
    name = "onCreateView",
    filters = listOf(
        string("READERA_PREF_SCREEN_ROOT"),
        methodCall(definingClass = PREFS_FRAGMENT_CLASS, opcode = Opcode.INVOKE_DIRECT, parameters = listOf()),
    ),
)

internal fun fullScreenFlagsInitFingerprint(definingClass: String, readingModePreference: FieldReference) = Fingerprint(
    definingClass = definingClass,
    name = "<init>",
    filters = listOf(
        fieldAccess(readingModePreference, Opcode.SGET_OBJECT),
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = "Landroid/content/SharedPreferences;", location = MatchAfterImmediately()),
        methodCall(returnType = "Ljava/lang/Boolean;", location = MatchAfterImmediately()),
        opcode(Opcode.IPUT_BOOLEAN, MatchAfterWithin(4)),
        opcode(Opcode.CONST_4, MatchAfterImmediately()),
        opcode(Opcode.IPUT_BOOLEAN, MatchAfterImmediately()),
    ),
)
