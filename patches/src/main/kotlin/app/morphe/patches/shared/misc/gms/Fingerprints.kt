/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object GetCredentialSuccessToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(string("GetCredentialSuccess(credential=")),
)

internal object GetCredentialFailureToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(string("GetCredentialFailure(type=")),
)

internal const val KOTLIN_RESULT_CLASS = "Lkotlin/Result;"

internal object ObfuscatedResultToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    filters = listOf(string("Success(")),
    custom = { _, classDef -> classDef.type != KOTLIN_RESULT_CLASS },
)
