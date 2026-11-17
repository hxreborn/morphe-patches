/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.splash

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance

internal object BeginFinishSplashGateFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        newInstance("Lcom/cardfeed/publicapp/splash/presentation/SplashViewModel\$beginFinishSplashGate\$2;"),
    ),
)

internal object HandleSplashEventFingerprint : Fingerprint(
    classFingerprint = BeginFinishSplashGateFingerprint,
    returnType = "Lkotlin/Unit;",
    parameters = listOf("L"),
)
