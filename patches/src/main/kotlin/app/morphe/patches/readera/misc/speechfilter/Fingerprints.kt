/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechfilter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object SpeakTextFingerprint : Fingerprint(
    strings = listOf("SpeechHelper speek [%s]"),
    filters = listOf(
        methodCall(definingClass = "Landroid/speech/tts/TextToSpeech;", name = "speak"),
    ),
)

internal object SynthesizeTextFingerprint : Fingerprint(
    filters = listOf(
        methodCall(definingClass = "Landroid/speech/tts/TextToSpeech;", name = "synthesizeToFile"),
    ),
)
