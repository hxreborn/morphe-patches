/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patches.readera.misc.highlightcolors.READ_ACTIVITY_CLASS
import app.morphe.patches.readera.misc.highlightcolors.SelectionActionsFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SELECTION_TEXT_RESULT_WINDOW = 1

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

internal object AppCompatAttachBaseContextFingerprint : Fingerprint(
    name = "attachBaseContext",
    returnType = "V",
    parameters = listOf(CONTEXT_CLASS),
    custom = { _, classDef -> !classDef.type.startsWith("Lapp/hxreborn/extension/") },
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(CONTEXT_CLASS), returnType = CONTEXT_CLASS),
        methodCall(opcode = Opcode.INVOKE_SUPER, definingClass = "Landroid/app/Activity;", name = "attachBaseContext"),
    ),
)

internal object ReadAloudFromCurrentPositionFingerprint : Fingerprint(
    definingClass = READ_ACTIVITY_CLASS,
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = READ_ACTIVITY_CLASS, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(name = "<init>", location = MatchAfterImmediately()),
        methodCall(definingClass = READ_ACTIVITY_CLASS, parameters = listOf("L", "Z"), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal object ClipboardCopyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(methodCall(definingClass = "Landroid/content/ClipboardManager;", name = "setPrimaryClip")),
)

internal fun selectionCopyFingerprint(clipboard: MethodReference) = Fingerprint(
    classFingerprint = SelectionActionsFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_DIRECT, parameters = listOf(), returnType = "Z"),
        methodCall(opcode = Opcode.INVOKE_DIRECT, parameters = listOf("Z"), returnType = "Ljava/lang/String;"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterWithin(SELECTION_TEXT_RESULT_WINDOW),
        ),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = READ_ACTIVITY_CLASS, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = READ_ACTIVITY_CLASS,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        methodCall(opcode = Opcode.INVOKE_STATIC, definingClass = clipboard.definingClass, name = clipboard.name),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = READ_ACTIVITY_CLASS, location = MatchAfterImmediately()),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;", "I"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object SelectionMenusFingerprint : Fingerprint(
    classFingerprint = SelectionActionsFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        checkCast("Landroid/widget/ScrollView;"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/widget/ScrollView;", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/ScrollView;"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/widget/ScrollView;", location = MatchAfterImmediately()),
    ),
)

internal object SelectionPositionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L", "L"),
    strings = listOf("PositionHelper create: %f: %s - %s [%f:%f]"),
)

internal fun documentAccessorFingerprint(documentType: String) = Fingerprint(
    definingClass = READ_ACTIVITY_CLASS,
    returnType = documentType,
    parameters = listOf(),
)

internal fun selectionTextFingerprint(text: MethodReference) = Fingerprint(
    definingClass = text.definingClass,
    name = text.name,
    returnType = text.returnType,
    parameters = text.parameterTypes.map { it.toString() },
    filters = listOf(fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchFirst())),
)
