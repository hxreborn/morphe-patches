/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.highlightcolors

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val READ_WIDGET_PACKAGE = "Lorg/readera/read/widget/"
internal const val DEFAULT_QUOTE_HIGHLIGHT = 0x601eabdf

internal object QuoteColorIconFingerprint : Fingerprint(
    definingClass = READ_WIDGET_PACKAGE,
    returnType = "V",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("I"),
            returnType = "Landroid/graphics/PorterDuffColorFilter;",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object SelectionActionsFingerprint : Fingerprint(
    definingClass = READ_WIDGET_PACKAGE,
    name = "<init>",
    parameters = listOf(READ_ACTIVITY_CLASS, "L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, name = "findViewById"),
        checkCast("Landroid/view/ViewGroup;", MatchAfterWithin(1)),
        opcode(Opcode.CONST, MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, name = "findViewById", location = MatchAfterImmediately()),
        opcode(Opcode.CONST, MatchAfterWithin(2)),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, name = "findViewById", location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE),
        opcode(Opcode.INVOKE_DIRECT, MatchAfterImmediately()),
        opcode(Opcode.IPUT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf(READ_ACTIVITY_CLASS, "L"),
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.IPUT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf(READ_ACTIVITY_CLASS, "L"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal const val QUOTE_GROUP_ID_MATCH = 2
internal const val FIND_VIEW_MATCH = 3
internal const val DICTIONARY_GROUP_ID_MATCH = 4
internal const val QUOTE_PICKER_MATCH = 9
internal const val DICTIONARY_PICKER_MATCH = 12

internal object QuoteHighlightColorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED),
    returnType = "I",
    filters = listOf(literal(DEFAULT_QUOTE_HIGHLIGHT)),
)

internal object NightHighlightFingerprint : Fingerprint(
    classFingerprint = QuoteHighlightColorFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Lorg/readera/read/widget/ReadSurface;", name = "getColorMode"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, location = MatchAfterImmediately()),
        literal(15),
    ),
)

internal object CreateQuoteFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf(READ_ACTIVITY_CLASS, "L", "I", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/Collections;", name = "sort"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = READ_ACTIVITY_CLASS,
            parameters = listOf(),
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal fun defaultColorWriteFingerprint(defaultColorField: FieldReference) = Fingerprint(
    definingClass = defaultColorField.definingClass,
    name = "<init>",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = "Landroid/content/SharedPreferences;"),
        fieldAccess(
            opcode = Opcode.IPUT,
            definingClass = defaultColorField.definingClass,
            name = defaultColorField.name,
            type = "I",
        ),
    ),
)

internal fun prefsReloadFingerprint(prefsClass: String) = Fingerprint(
    definingClass = prefsClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(newInstance(prefsClass)),
)

internal fun paletteByIndexFingerprint(paletteClass: String) = Fingerprint(
    definingClass = paletteClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = paletteClass,
    parameters = listOf("I"),
)

internal fun paletteConstructorFingerprint(paletteClass: String) = Fingerprint(
    definingClass = paletteClass,
    name = "<init>",
    parameters = listOf("Ljava/lang/String;", "I", "I", "Ljava/lang/String;", "I", "I", "I"),
)

internal fun prefsStoreFingerprint(prefsClass: String) = Fingerprint(
    definingClass = prefsClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Landroid/content/SharedPreferences;",
    parameters = listOf(),
)

internal object DictionaryWordAddFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf(READ_ACTIVITY_CLASS, "L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(READ_ACTIVITY_CLASS, "L", "I"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object DictionaryWordColorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L", "I"),
    strings = listOf("DictModel saveColor key:%s, color:%d"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT, type = "I")),
)

internal object DictionaryWordEventFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    strings = listOf("EventDictWordProcessed added "),
)

internal object DictionaryUnavailableFingerprint : Fingerprint(
    classFingerprint = SelectionActionsFingerprint,
    returnType = "V",
    parameters = listOf("L", "Landroid/view/View;"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_DIRECT, returnType = "Z", parameters = listOf()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;", "I"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun dictionaryUnavailableCheckFingerprint(check: MethodReference) = Fingerprint(
    definingClass = check.definingClass,
    name = check.name,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.CONST_CLASS),
        methodCall(
            parameters = listOf("Ljava/lang/Class;"),
            returnType = "Ljava/lang/Object;",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun dictionaryWordHintFingerprint(addWord: MethodReference) = Fingerprint(
    classFingerprint = SelectionActionsFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = addWord.definingClass, name = addWord.name),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = READ_ACTIVITY_CLASS,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterWithin(1),
        ),
    ),
)
