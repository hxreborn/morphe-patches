/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.drawPullRefreshAsLoadingIndicator
import app.morphe.patches.shared.misc.smali.descriptor

private const val ANDROID_VIEW_PARAMETERS = 6

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "Material 3 loading indicator",
    description = "Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch)

    execute {
        drawPullRefreshAsLoadingIndicator(PullToRefreshContentFingerprint, ANDROID_VIEW_PARAMETERS) { producerType, producer, progress, _ ->
            val produce = classDefBy(producerType).methods.single { it.returnType == "F" && it.parameterTypes.isEmpty() }
            """
                invoke-interface { $producer }, ${descriptor(produce)}
                move-result $progress
            """
        }
    }
}
