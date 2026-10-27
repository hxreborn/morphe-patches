/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.platform

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val STRING_TYPE = "Ljava/lang/String;"

private fun addHeader(location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()) =
    anyInstruction(
        methodCall(definingClass = "Lokhttp3/Request\$Builder;", name = "addHeader"),
        methodCall(parameters = listOf(STRING_TYPE, STRING_TYPE), returnType = "V"),
        location = location,
    )

internal object AiRequestInterceptorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(definingClass = "La/bd/jniutils/TokenUtils;", name = "f"),
        addHeader(),
        methodCall(
            parameters = listOf(STRING_TYPE, STRING_TYPE),
            returnType = STRING_TYPE,
            location = MatchAfterWithin(4),
        ),
        methodCall(
            parameters = listOf(STRING_TYPE, STRING_TYPE),
            returnType = STRING_TYPE,
            location = MatchAfterWithin(4),
        ),
        addHeader(MatchAfterWithin(4)),
    ),
)
