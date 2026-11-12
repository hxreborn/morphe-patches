/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.fonts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val FONTS_ACTIVITY_CLASS = "Lorg/readera/FontsActivity;"

internal object SaveCustomFontsCacheFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
    strings = listOf("custom", "remote", "FontsModel saveCustomCache %s"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.IGET_OBJECT, MatchAfterImmediately()),
    ),
)

internal object SystemFontsFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        string("system"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/lang/String;"), returnType = "Ljava/util/List;"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf()),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "Ljava/util/List;", location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "Ljava/util/Map;", location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/util/List;", "Ljava/util/Map;"), location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/util/List;"), returnType = "V", location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/io/Closeable;"), location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal object SystemFontsCacheValidFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("preset", "/system/fonts"),
    filters = listOf(
        methodCall(parameters = listOf("Ljava/lang/String;"), returnType = "Z"),
        methodCall(parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"), returnType = "Z"),
    ),
)

internal object SystemFontsScanFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    returnType = "Ljava/util/List;",
    strings = listOf(" UI"),
    filters = listOf(
        string("/system/fonts"),
        methodCall(returnType = "Ljava/util/List;", location = MatchAfterImmediately()),
    ),
)

internal object FontsUpdateFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("FontsModel update GO"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/util/Map;", "Ljava/util/List;"), returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/util/Map;", "Ljava/util/List;"), returnType = "V", location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Ljava/util/Map;", "Ljava/util/List;"), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal object FontsByCategoryFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    returnType = "Ljava/util/List;",
    parameters = listOf("I"),
    filters = listOf(
        literal(3),
        opcode(Opcode.IF_NE, MatchAfterImmediately()),
        newInstance("Ljava/util/ArrayList;", MatchAfterImmediately()),
        opcode(Opcode.INVOKE_DIRECT, MatchAfterImmediately()),
    ),
)

internal object FontNamesFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    returnType = "Ljava/util/Map;",
    parameters = listOf(),
    filters = listOf(
        newInstance("Ljava/util/HashMap;"),
        opcode(Opcode.INVOKE_DIRECT, MatchAfterImmediately()),
        newInstance("Ljava/util/ArrayList;", MatchAfterImmediately()),
        opcode(Opcode.INVOKE_DIRECT, MatchAfterImmediately()),
    ),
)

internal object LoadFontsFingerprint : Fingerprint(
    classFingerprint = SaveCustomFontsCacheFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf(),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I", "I"), returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I", "I"), returnType = "V", location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I", "I"), returnType = "V", location = MatchAfterWithin(1)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I", "I"), returnType = "V", location = MatchAfterWithin(1)),
    ),
)

internal object FontFolderRowFingerprint : Fingerprint(
    definingClass = FONTS_ACTIVITY_CLASS,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Landroid/view/View;", name = "setOnClickListener"),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/widget/TextView;", name = "setText", parameters = listOf("I"), location = MatchAfterImmediately()),
    ),
)

internal object FontFolderClickFingerprint : Fingerprint(
    definingClass = FONTS_ACTIVITY_CLASS,
    returnType = "V",
    parameters = listOf(FONTS_ACTIVITY_CLASS, "Landroid/view/View;"),
    strings = listOf("fonts"),
)
