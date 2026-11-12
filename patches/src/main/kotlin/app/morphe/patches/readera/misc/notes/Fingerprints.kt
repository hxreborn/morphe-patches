/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.notes

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val DICTIONARY_CALL_WINDOW = 4
private const val NOTE_POSITION_POST_WINDOW = 3

internal object DrawerNotesClickFingerprint : Fingerprint(
    name = "onMenuItemClick",
    returnType = "Z",
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Landroidx/fragment/app/"), returnType = "V"),
        methodCall(definingClass = "Lorg/readera/DictActivity;", location = MatchAfterWithin(DICTIONARY_CALL_WINDOW)),
    ),
)

internal object QuoteListFingerprint : Fingerprint(
    strings = listOf("AboutDocActivity citation pos=%d"),
)

internal object QuoteJumpFingerprint : Fingerprint(
    classFingerprint = QuoteListFingerprint,
    returnType = "V",
    parameters = listOf("L", "Landroid/view/View;"),
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L", "J"), returnType = "V", location = MatchAfterWithin(NOTE_POSITION_POST_WINDOW)),
    ),
)

internal object BookmarkListFingerprint : Fingerprint(
    strings = listOf("AboutDocActivity bookmark pos=%d"),
)

internal object BookmarkJumpFingerprint : Fingerprint(
    classFingerprint = BookmarkListFingerprint,
    returnType = "V",
    parameters = listOf("L", "Landroid/view/View;"),
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L", "J"), returnType = "V", location = MatchAfterWithin(NOTE_POSITION_POST_WINDOW)),
    ),
)
