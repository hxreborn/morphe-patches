/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.reactnative

import app.morphe.patcher.Fingerprint

internal object ScrollViewLayoutFingerprint : Fingerprint(
    name = "onLayout",
    parameters = listOf("Z", "I", "I", "I", "I"),
    returnType = "V",
    custom = { _, classDef ->
        classDef.superclass == "Landroid/widget/ScrollView;" &&
            classDef.type.startsWith("Lcom/facebook/react/views/scroll/")
    },
)
