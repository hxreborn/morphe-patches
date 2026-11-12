/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.descriptions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val MINIMUM_DESCRIPTION_LENGTH = 50L
private const val MAXIMUM_DESCRIPTION_LENGTH = 1500L

internal object BookDescriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("L", "Ljava/lang/String;"),
    filters = listOf(
        string("DocMetaUtils normalizeAnnotation: len: %d, input: "),
        literal(MINIMUM_DESCRIPTION_LENGTH),
        methodCall(parameters = listOf("Ljava/lang/String;"), returnType = "Ljava/lang/String;"),
        literal(MAXIMUM_DESCRIPTION_LENGTH),
        methodCall(definingClass = "Ljava/lang/String;", name = "substring", parameters = listOf("I", "I")),
    ),
)
