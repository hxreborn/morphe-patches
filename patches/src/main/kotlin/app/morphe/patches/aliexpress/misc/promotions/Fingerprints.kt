/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/coupons/RemoveCouponsPopupPatch.kt
 */
package app.morphe.patches.aliexpress.misc.promotions

import app.morphe.patcher.Fingerprint

internal object PromotionActivityFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/app/Activity;"),
    strings = listOf("isValidActivity, sForbiddenAllPopInDebugMode true, return false"),
)

internal object PromotionEventFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Ljava/lang/String;", "Z"),
    strings = listOf("isValidActivity, sForbiddenAllPopInDebugMode true, return false"),
)
