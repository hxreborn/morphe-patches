/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object CachedClientBuilderFingerprint : Fingerprint(
    returnType = "Lokhttp3/OkHttpClient\$Builder;",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("http-cache"),
    filters = listOf(
        methodCall(
            parameters = listOf(),
            returnType = "Lokhttp3/OkHttpClient\$Builder;",
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)
