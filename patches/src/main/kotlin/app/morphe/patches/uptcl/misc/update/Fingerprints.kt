/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.update

import app.morphe.patcher.Fingerprint

internal object AppVersionResultFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "Lkotlin/coroutines/Continuation;"),
    strings = listOf("200", "008", "007"),
)
