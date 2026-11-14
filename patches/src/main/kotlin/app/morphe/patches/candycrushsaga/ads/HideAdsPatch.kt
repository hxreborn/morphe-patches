/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val SDK_CLASS = "Lcom/king/abm/adprovider/AdProviderSDKAndroid;"
private const val MODULE_CLASS = "Lcom/king/abm/adprovider/AdProviderModuleAndroid;"
private const val NO_FILL_ERROR_CODE = 0x3
private const val DISABLED_MESSAGE = "ads disabled"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Stops rewarded and interstitial ads from loading.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)

    execute {
        AdSdkInitFingerprint.matchSingle().method.addInstructions(
            0,
            """
                iput-wide p1, p0, $SDK_CLASS->mAdProviderAddress:J
                const-string p1, "$DISABLED_MESSAGE"
                invoke-virtual { p0, p1 }, $SDK_CLASS->onSDKInitFailed(Ljava/lang/String;)V
                return-void
            """,
        )
        AdSdkCollectBidderTokenFingerprint.matchSingle().method.addInstructions(
            0,
            """
                iget-wide v0, p0, $SDK_CLASS->mAdProviderAddress:J
                const/4 v2, 0x0
                const-string v3, "$DISABLED_MESSAGE"
                invoke-static { v0, v1, p1, v2, v3 }, $SDK_CLASS->collectBidderTokenJNI(JILjava/lang/String;Ljava/lang/String;)V
                return-void
            """,
        )
        AdLoadFingerprint.matchSingle().method.addInstructions(
            0,
            """
                iput-wide p1, p0, $MODULE_CLASS->mAdProviderAddress:J
                const/16 p1, $NO_FILL_ERROR_CODE
                const-string p2, "$DISABLED_MESSAGE"
                invoke-virtual { p0, p1, p2 }, $MODULE_CLASS->onLoadFailed(ILjava/lang/String;)V
                return-void
            """,
        )
    }
}
