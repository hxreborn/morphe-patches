/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object SecurityContextInitializerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "[Ljava/lang/Object;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/alibaba/wireless/security/framework/utils/UserTrackMethodJniBridge;",
            name = "init",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ),
    ),
)
