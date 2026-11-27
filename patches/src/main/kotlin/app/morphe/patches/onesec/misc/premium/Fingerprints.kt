/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.onesec.misc.premium

import app.morphe.patcher.Fingerprint

internal object EntitlementModelFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("EntitlementModel(activeProEntitlementSources="),
)
