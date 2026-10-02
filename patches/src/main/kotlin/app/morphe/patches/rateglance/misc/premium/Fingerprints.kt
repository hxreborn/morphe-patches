/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rateglance.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val BILLING_ENTITLEMENT_STATE_CLASS = "Lcom/sry/rateglance/domain/model/BillingEntitlementState;"

internal object PremiumAccessFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "J"),
    filters = listOf(
        fieldAccess(definingClass = "Ljava/lang/Boolean;", name = "TRUE"),
        methodCall(definingClass = "Ljava/lang/Long;", name = "longValue"),
    ),
    custom = { _, classDef -> classDef.methods.any { it.returnType == BILLING_ENTITLEMENT_STATE_CLASS } },
)
