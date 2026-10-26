/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val SUBSCRIPTION_CLASS = "Lcom/athan/calendar/data/model/AssociateUserSubscriptionSyncResponse;"

internal object StoredSubscriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = SUBSCRIPTION_CLASS,
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("premium_subscription_response"),
)
