/*
 * Copyright (C) 2026 Morphe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/31a66d95932f6ccf4e39a5804c750af72a606d15
 * Commit 31a66d95932f6ccf4e39a5804c750af72a606d15 (2026-09-11),
 * patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/Fingerprints.kt
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

internal object GooglePlayServicesAvailabilityFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("This should never happen.", "MetadataValueReader"),
)
