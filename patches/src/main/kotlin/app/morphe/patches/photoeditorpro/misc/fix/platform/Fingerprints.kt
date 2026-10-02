/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.platform

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val REQUEST_BUILDER_CLASS = "Lokhttp3/Request\$Builder;"
private const val STRING_TYPE = "Ljava/lang/String;"

internal object AiRequestInterceptorFingerprint : Fingerprint(
    name = "intercept",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lokhttp3/Response;",
    parameters = listOf("Lokhttp3/Interceptor\$Chain;"),
    filters = listOf(
        methodCall(definingClass = "La/bd/jniutils/TokenUtils;", name = "f"),
        methodCall(definingClass = REQUEST_BUILDER_CLASS, name = "addHeader"),
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
        methodCall(
            definingClass = REQUEST_BUILDER_CLASS,
            name = "addHeader",
            location = MatchAfterWithin(4),
        ),
    ),
)
