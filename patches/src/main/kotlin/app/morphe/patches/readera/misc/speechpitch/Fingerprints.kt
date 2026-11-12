/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechpitch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.readera.misc.speechrate.ApplySpeechRateFingerprint
import app.morphe.patches.readera.shared.TtsPrefsEventFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val INFLATE_ARGUMENT_WINDOW = 1

internal object PitchPreferencesViewFingerprint : Fingerprint(
    classFingerprint = TtsPrefsEventFingerprint,
    name = "onCreateView",
    filters = listOf(
        opcode(Opcode.CONST),
        methodCall(
            definingClass = "Landroid/view/LayoutInflater;",
            name = "inflate",
            location = MatchAfterWithin(INFLATE_ARGUMENT_WINDOW),
        ),
        opcode(Opcode.CONST),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById", location = MatchAfterImmediately()),
    ),
)

internal object PitchPreferencesResumeFingerprint : Fingerprint(
    classFingerprint = TtsPrefsEventFingerprint,
    name = "onResume",
)

internal object RateVoiceSelectionFingerprint : Fingerprint(
    classFingerprint = ApplySpeechRateFingerprint,
    filters = listOf(
        methodCall(parameters = listOf(), returnType = "Ljava/lang/String;", opcode = Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(type = "Ljava/lang/String;", opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately()),
        methodCall(parameters = listOf("Ljava/lang/String;"), opcode = Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
    ),
)

internal fun selectedVoiceFingerprint(voiceType: String) = Fingerprint(
    definingClass = voiceType,
    returnType = "Landroid/speech/tts/Voice;",
    parameters = listOf(),
)

internal object SpeechRateSummaryFingerprint : Fingerprint(
    classFingerprint = TtsPrefsEventFingerprint,
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/String;", name = "valueOf", parameters = listOf("F")),
        fieldAccess(type = "Landroid/widget/TextView;", opcode = Opcode.IGET_OBJECT),
    ),
)

internal fun speechRateRowFingerprint(summaryField: FieldReference) = Fingerprint(
    classFingerprint = TtsPrefsEventFingerprint,
    name = "onCreateView",
    filters = listOf(
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", MatchAfterImmediately()),
        fieldAccess(summaryField, Opcode.IPUT_OBJECT, MatchAfterImmediately()),
    ),
)
