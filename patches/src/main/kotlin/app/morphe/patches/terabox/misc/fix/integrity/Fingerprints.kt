/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.fix.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal const val STANDARD_INTEGRITY_TOKEN_PROVIDER =
    "Lcom/google/android/play/core/integrity/StandardIntegrityManager\$StandardIntegrityTokenProvider;"

internal object StandardIntegrityTokenRequestFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("v_rr_gg_req_tk_start", "v_rr_gg_req_tk_prov_null"),
)

internal object ClassicIntegrityTokenRequestFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/play/core/integrity/IntegrityManager;",
            name = "requestIntegrityToken",
        ),
    ),
    custom = { _, classDef ->
        !classDef.type.startsWith("Lcom/google/") && !classDef.type.startsWith("Lcom/appsflyer/")
    },
)
