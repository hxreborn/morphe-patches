/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.ads

import app.morphe.patcher.Fingerprint

internal object DashboardBannerItemsFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(", bannerItems="),
    custom = { _, classDef ->
        classDef.methods.any {
            it.name == "getBannerItems" && it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/List;"
        }
    },
)
