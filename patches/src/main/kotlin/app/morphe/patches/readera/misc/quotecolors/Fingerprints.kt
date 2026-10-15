/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.quotecolors

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

private const val READ_ACTIVITY_CLASS = "Lorg/readera/read/ReadActivity;"
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
    ),
)

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

internal fun defaultQuoteColorWriteFingerprint(defaultColorField: FieldReference) = Fingerprint(
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
