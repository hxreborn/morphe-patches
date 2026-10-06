/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.anytracker.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val PLAN_CLASS = "Lcom/shervinkoushan/anyTracker/core/profile/Plan;"
internal const val CACHE_FETCH_POLICY_CLASS = "Lcom/revenuecat/purchases/CacheFetchPolicy;"

private val planConstantRead = fieldAccess(definingClass = PLAN_CLASS, type = PLAN_CLASS, opcode = Opcode.SGET_OBJECT)

private val fetchCurrentPolicy = fieldAccess(
    definingClass = CACHE_FETCH_POLICY_CLASS,
    name = "FETCH_CURRENT",
    opcode = Opcode.SGET_OBJECT,
)

internal object FetchPlanFingerprint : Fingerprint(
    filters = listOf(
        methodCall(definingClass = "Lcom/revenuecat/purchases/Purchases;", name = "invalidateCustomerInfoCache"),
        fetchCurrentPolicy,
        methodCall(definingClass = "Lcom/revenuecat/purchases/CoroutinesExtensionsKt;", name = "awaitCustomerInfo"),
        planConstantRead,
    ),
)

internal object RequestCustomerInfoFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        fetchCurrentPolicy,
        methodCall(
            definingClass = "Lcom/revenuecat/purchases/ListenerConversionsKt;",
            name = "getCustomerInfoWith",
        ),
    ),
)

internal object StoredPlanDefaultFingerprint : Fingerprint(
    strings = listOf("ll-plan"),
    filters = listOf(string("BASIC")),
)

internal fun planStateConstructorFingerprint(viewModelClass: String) = Fingerprint(
    definingClass = viewModelClass,
    name = "<init>",
    filters = listOf(planConstantRead),
)
