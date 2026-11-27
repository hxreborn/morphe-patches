/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medal.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import com.android.tools.smali.dexlib2.AccessFlags

private const val PREMIUM_PROMOTION_COMPOSE_GROUP = 515331709

internal object PremiumPromotionRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lkotlin/jvm/functions/Function0;", "L", "I"),
    filters = listOf(literal(PREMIUM_PROMOTION_COMPOSE_GROUP)),
)
