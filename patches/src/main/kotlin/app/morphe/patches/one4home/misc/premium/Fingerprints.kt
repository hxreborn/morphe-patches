/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.one4home.misc.premium

import app.morphe.patcher.Fingerprint

internal object ProBillingStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("ProBillingState(isPro="),
)

internal object HomePalCatalogFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("one4home_pal_founder"),
)
