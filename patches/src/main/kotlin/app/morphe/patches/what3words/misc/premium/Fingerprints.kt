/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.what3words.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val MUTABLE_STATE_FLOW = "Lkotlinx/coroutines/flow/MutableStateFlow;"

internal object SubscriptionRepositoryFingerprint : Fingerprint(
    filters = listOf(string("Fetch subscription error")),
)

internal object UpdatePlanInfoFingerprint : Fingerprint(
    classFingerprint = SubscriptionRepositoryFingerprint,
    isStatic = false,
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        string("planInfo"),
        methodCall(definingClass = MUTABLE_STATE_FLOW, name = "compareAndSet"),
    ),
)
