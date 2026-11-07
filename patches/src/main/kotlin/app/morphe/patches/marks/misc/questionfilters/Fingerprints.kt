/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.questionfilters

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance

internal object ChapterQuestionsRequestFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(methodCall(definingClass = "Lfv;", name = "M1")),
)

internal object GsonBuilderFingerprint : Fingerprint(
    filters = listOf(
        newInstance("Lcom/google/gson/GsonBuilder;"),
        newInstance("Lcom/scoremarks/marks/other/SafeTypeAdapterFactory;"),
        methodCall(definingClass = "Lcom/google/gson/GsonBuilder;", name = "registerTypeAdapterFactory"),
    ),
)
