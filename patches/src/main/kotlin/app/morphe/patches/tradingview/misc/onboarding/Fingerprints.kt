/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.onboarding

import app.morphe.patcher.Fingerprint

internal object OnboardingAvailabilityFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/feature/onboarding/impl/interactor/OnboardingAvailabilityInteractorImpl;",
    name = "needShowOnboarding",
    returnType = "Ljava/lang/Object;",
)
