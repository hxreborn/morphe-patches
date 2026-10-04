/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes app open, interstitial and native ads, and the promoted apps list.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)
    dependsOn(failAdMobLoadsPatch)

    execute {
        val applyRemoteConfig = ApplyRemoteConfigFingerprint.matchSingle()
        val switches = applyRemoteConfig.instructionMatches.map {
            it.getInstruction<ReferenceInstruction>().reference
        }

        fun MutableMethod.turnOffSwitches(index: Int) {
            val register = getFreeRegisterProvider(index, 1).getFreeRegister()
            addInstructions(
                index,
                "const-string v$register, \"no\"\n" +
                    switches.joinToString("\n") { "sput-object v$register, $it" },
            )
        }

        val loadMoreApps = LoadMoreAppsFingerprint.matchSingle().method
        applyRemoteConfig.method.apply {
            turnOffSwitches(
                indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>() == loadMoreApps
                },
            )
        }
        ApplicationOnCreateFingerprint.matchSingle().method.turnOffSwitches(0)
        loadMoreApps.returnEarly()
    }
}
