/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.backup

import app.morphe.patcher.Fingerprint

internal object RetrofitCreateFingerprint : Fingerprint(
    definingClass = "Lretrofit2/Retrofit;",
    name = "create",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Class;"),
)
