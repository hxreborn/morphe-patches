/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.WebEngageShouldRenderFingerprint

private const val DASHBOARD_BANNER_MODEL_COUNT = 2

@Suppress("unused")
val removeAdsPatch: BytecodePatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes promotional banners and in-app messages.",
) {
    compatibleWith(AppCompatibilities.MARKS)

    execute {
        DashboardBannerItemsFingerprint.matchAll(DASHBOARD_BANNER_MODEL_COUNT..DASHBOARD_BANNER_MODEL_COUNT)
            .forEach { match ->
                val getter = match.classDef.methods.single {
                    it.name == "getBannerItems" && it.parameterTypes.isEmpty()
                }
                val register = getter.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                getter.addInstructions(
                    0,
                    """
                        invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                        move-result-object v$register
                        return-object v$register
                    """,
                )
            }
        WebEngageShouldRenderFingerprint.matchSingle().method.returnEarly(false)
    }
}
