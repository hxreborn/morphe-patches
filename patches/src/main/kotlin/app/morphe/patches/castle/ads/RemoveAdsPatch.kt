/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castle.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.secshell.SecShellPayload
import app.morphe.patches.shared.misc.secshell.editSecShellPayload
import app.morphe.patches.shared.misc.secshell.secShellPayloadPatch
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AD_LOCATION_CLASS = "Lcom/gxgx/daqiandy/bean/AdsStateBean;"
private const val AD_PLACEMENT_CLASS = "Lcom/gxgx/daqiandy/bean/AdsMaxStateBean;"
private const val AD_SDK_CLASS = "Lcom/secmtp/sdk/core/api/ATSDK;"
private val AUTO_AD_CLASSES = listOf(
    "Lcom/secmtp/sdk/interstitial/api/ATInterstitialAutoAd;",
    "Lcom/secmtp/sdk/rewardvideo/api/ATRewardVideoAutoAd;",
)

private const val REWARD_FLOW_LOG = "maxAd====show======adType=="
private const val REWARD_CLOSE_LOG = "self ads close"

private const val PLACEMENT_DISABLED = """
    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
    return-object v0
"""

private fun SecShellPayload.completeRewardedActions() {
    val rewardFlow = methodUsingString(REWARD_FLOW_LOG)
    val completions = referencesInMethodUsing(REWARD_CLOSE_LOG).filterIsInstance<MethodReference>()
    val (listener, complete) = instanceFields(rewardFlow.definingClass)
        .firstNotNullOfOrNull { field ->
            completions.firstOrNull { it.definingClass == field.type }?.let { field to it }
        } ?: throw PatchException("No reward listener call in ${rewardFlow.definingClass}")

    rewardFlow.addInstructionsWithLabels(
        0,
        """
            move-object/from16 v0, p0
            iget-object v0, v0, ${descriptor(listener)}
            if-eqz v0, :done
            invoke-interface { v0 }, ${descriptor(complete)}
            :done
            return-void
        """,
    )
}

@Suppress("unused")
val removeAdsPatch = rawResourcePatch(
    name = "Remove ads",
    description = "Removes banner, video, interstitial and rewarded ads. Actions that waited on a rewarded ad run immediately.",
) {
    compatibleWith(AppCompatibilities.CASTLE)

    dependsOn(secShellPayloadPatch)

    execute {
        editSecShellPayload {
            completeRewardedActions()
            method(AD_LOCATION_CLASS, "getEnable").returnEarly(false)
            replaceBody(AD_PLACEMENT_CLASS, "getStatus", registers = 2, smali = PLACEMENT_DISABLED)
            methods(AD_SDK_CLASS, "init").forEach { it.returnEarly() }
            AUTO_AD_CLASSES.forEach { autoAd ->
                methods(autoAd, "init").forEach { it.returnEarly() }
                methods(autoAd, "show").forEach { it.returnEarly() }
                method(autoAd, "isAdReady").returnEarly(false)
            }
        }
    }
}
