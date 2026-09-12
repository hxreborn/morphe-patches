/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

private const val COMMON_PREF_MANAGER_CLASS = "Lcom/oneweather/common/preference/CommonPrefManager;"

internal object PremiumUserFingerprint : Fingerprint(
    definingClass = COMMON_PREF_MANAGER_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("isPremiumUser"),
)

internal object PremiumUserFlowFingerprint : Fingerprint(
    definingClass = COMMON_PREF_MANAGER_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lkotlinx/coroutines/flow/Flow;",
    parameters = emptyList(),
    strings = listOf("isPremiumUser"),
)

internal object SyncPremiumDataFingerprint : Fingerprint(
    definingClass = "Lcom/oneweather/premium/domain/usecase/SyncPremiumDataUseCase;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Z",
        "Ljava/lang/String;",
        "Lcom/oneweather/premium/data/model/PurchaseTokenDetails;",
        "Lkotlin/coroutines/Continuation;",
    ),
)

internal object UserPlanFingerprint : Fingerprint(
    definingClass = "Lcom/oneweather/premium/data/pref/EntitlementsPrefManager;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/oneweather/premium/domain/model/UserPlan;",
    parameters = emptyList(),
    strings = listOf("user_plan"),
)
