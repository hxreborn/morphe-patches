/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.signin

import app.morphe.patcher.Fingerprint

internal object GoogleSignInEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/feature/auth/impl/base/interactor/GoogleSignInInteractorImpl;",
    name = "isGoogleSignInEnabled",
    returnType = "Z",
)
