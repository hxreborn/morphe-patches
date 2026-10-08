/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Blocks usage statistics sent to imo, AppsFlyer, Firebase and Facebook, and the advertising ID upload.",
) {
    compatibleWith(AppCompatibilities.IMO)
    dependsOn(disableAnalyticsCollectionPatch)

    execute {
        listOf(AppsFlyerStartFingerprint, AppsFlyerEventFingerprint).forEach { fingerprint ->
            fingerprint.matchAllMethodIndicesForEach { index -> replaceInstruction(index, "nop") }
        }
        FirebaseCollectionSettingFingerprint.matchSingle().method.returnEarly(false)
        MonitorEventFingerprint.matchSingle().method.apply {
            val callback = classDefBy(parameterTypes.last().toString()).methods.single {
                AccessFlags.ABSTRACT.isSet(it.accessFlags)
            }
            addInstructionsWithLabels(
                0,
                """
                    if-eqz p2, :done
                    move-object/from16 v0, p2
                    new-instance v1, Lorg/json/JSONObject;
                    invoke-direct { v1 }, Lorg/json/JSONObject;-><init>()V
                    invoke-virtual { v0, v1 }, $callback
                    :done
                    return-void
                """,
            )
        }
        listOf(StatEventFingerprint, AdvertisingIdUploadFingerprint).forEach { it.matchSingle().method.returnEarly() }
    }
}
