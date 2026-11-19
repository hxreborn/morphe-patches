/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blockit.misc.parachutes

import app.morphe.patcher.Fingerprint

internal object LaunchPurchaseFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("billing not ready, deferring launch: "),
)

internal object ParachutesBoughtFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    strings = listOf("bought parachutes: "),
)

internal object ParachuteCountSetterFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("parachute_count", "parachute_sig"),
)

internal object ParachuteCountGetterFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf(),
    strings = listOf("getParachuteCount: signature mismatch, resetting"),
)
