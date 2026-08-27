/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.platform

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val OBFUSCATED_PLATFORM_KEY = "GWxRdBFvOG0="
internal const val OBFUSCATED_ANDROID_VALUE = "FG5QchhpZA=="

internal object AiRequestInterceptorFingerprint : Fingerprint(
    name = "intercept",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lokhttp3/Response;",
    parameters = listOf("Lokhttp3/Interceptor\$Chain;"),
    strings = listOf(OBFUSCATED_PLATFORM_KEY, OBFUSCATED_ANDROID_VALUE),
)
