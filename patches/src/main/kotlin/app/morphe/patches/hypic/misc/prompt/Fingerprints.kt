/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.prompt

import app.morphe.patcher.Fingerprint

internal const val INTERCEPTOR_CHAIN = "Lcom/bytedance/retrofit2/intercept/Interceptor${'$'}Chain;"
internal const val RISK_MODAL_BUNDLE = "riskModal"

internal object RiskResponseInterceptorFingerprint : Fingerprint(
    name = "intercept",
    returnType = "Lcom/bytedance/retrofit2/SsResponse;",
    parameters = listOf(INTERCEPTOR_CHAIN),
    strings = listOf("inTargetPath path => "),
)

internal object RiskModalDeepLinkFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/net/Uri;", "Ljava/util/HashMap;", "Landroid/content/Context;"),
    strings = listOf(RISK_MODAL_BUNDLE, "/subscribe"),
)
