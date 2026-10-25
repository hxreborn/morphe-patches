/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.joyn.ads

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes ads before and during videos. Live TV is unavailable from outside Joyn's supported countries.",
) {
    compatibleWith(AppCompatibilities.JOYN, AppCompatibilities.JOYN_AT)

    execute {
        val adsDisabledLogLambdaType = AdsDisabledLogMessageFingerprint.matchSingle().classDef.type
        val playerFactoryType = adsDisabledLogLambdaType.substringBefore('$') + ";"
        val match = enableAdsCheckFingerprint(playerFactoryType).matchSingle()
        val enableAdsResult = match.instructionMatches.last()
        val register = enableAdsResult.getInstruction<OneRegisterInstruction>().registerA

        match.method.replaceInstruction(enableAdsResult.index, "const/4 v$register, 0x0")
    }
}
