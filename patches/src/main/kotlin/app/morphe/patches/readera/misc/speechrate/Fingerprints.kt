/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechrate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val SPEECH_RATE_VIEW_CAST_WINDOW = 1
private const val SPEECH_RATE_LABEL_ID_WINDOW = 1

internal object ApplySpeechRateFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("PrefsTtsRateFragment setSpeechRate %f"),
)

internal const val SPEECH_RATE_ROOT_MATCH = 0
internal const val SPEECH_RATE_SEEK_BAR_ID_MATCH = 1
internal const val SPEECH_RATE_LABEL_ID_MATCH = 4

internal object SpeechRateRowFingerprint : Fingerprint(
    classFingerprint = ApplySpeechRateFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/view/View;"),
        opcode(Opcode.CONST, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/SeekBar;", MatchAfterWithin(SPEECH_RATE_VIEW_CAST_WINDOW)),
        opcode(Opcode.CONST, MatchAfterWithin(SPEECH_RATE_LABEL_ID_WINDOW)),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", MatchAfterWithin(SPEECH_RATE_VIEW_CAST_WINDOW)),
        methodCall(definingClass = "Landroid/widget/ProgressBar;", name = "setProgress"),
        methodCall(definingClass = "Landroid/view/View;", name = "setOnClickListener"),
    ),
)

internal object SpeechRateViewFingerprint : Fingerprint(
    classFingerprint = ApplySpeechRateFingerprint,
    name = "onCreateView",
    filters = listOf(
        literal(8),
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/view/View;", name = "setVisibility", location = MatchAfterImmediately()),
    ),
)
