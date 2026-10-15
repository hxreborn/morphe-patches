/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patches.netanalyzer.shared.composable

internal object AdsHostFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable("AdsHost")),
)

internal object AdSlotFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable("AdSlot")),
)

internal object ProUpsellRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable("ProUpsellRow")),
)
