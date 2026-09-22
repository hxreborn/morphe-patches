/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atlomaps.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import com.android.tools.smali.dexlib2.AccessFlags

private const val PREMIUM_FEATURE_BIT = 256

internal object BillingManagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("com.android.vending.billing.InAppBillingService.BIND"),
)

internal object OwnsFeatureFingerprint : Fingerprint(
    classFingerprint = BillingManagerFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "I"),
)

internal object PremiumEnabledFingerprint : Fingerprint(
    classFingerprint = BillingManagerFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(literal(PREMIUM_FEATURE_BIT)),
)
