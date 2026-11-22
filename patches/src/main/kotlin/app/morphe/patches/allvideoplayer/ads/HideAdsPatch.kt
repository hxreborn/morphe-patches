/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val hideAdsPatch = patch(
    name = "Hide ads",
    description = "Removes app open, interstitial and native ads, and the promoted apps list.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)
    dependsOn(failAdMobLoadsPatch)

    execute {
        val applyRemoteConfig = ApplyRemoteConfigFingerprint.match()
        val switches = applyRemoteConfig.instructionMatches.map { it.fieldReference }

        fun MutableMethod.turnOffSwitches(index: Int) {
            val register = getFreeRegisterProvider(index, 1).getFreeRegister()
            addInstructions(
                index,
                "const-string v$register, \"no\"\n" +
                    switches.joinToString("\n") { "sput-object v$register, $it" },
            )
        }

        val loadMoreApps = LoadMoreAppsFingerprint.method
        applyRemoteConfig.method.apply {
            turnOffSwitches(
                indexOfFirstInstructionOrThrow(
                    methodCall(smali = descriptor(loadMoreApps), opcode = Opcode.INVOKE_STATIC),
                ),
            )
        }
        ApplicationOnCreateFingerprint.method.turnOffSwitches(0)
        loadMoreApps.returnEarly()
    }
}
