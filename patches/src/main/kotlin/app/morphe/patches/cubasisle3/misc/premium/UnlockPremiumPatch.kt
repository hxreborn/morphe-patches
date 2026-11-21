/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cubasisle3.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patches.shared.misc.extension.cubasisle3UnlockPremiumExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/cubasisle3/StoreUnlock;"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks the full Cubasis version and the in-app shop content.",
) {
    dependsOn(cubasisle3UnlockPremiumExtensionPatch)
    compatibleWith(AppCompatibilities.CUBASIS_LE_3)
    execute {
        val appLoaded = AppLoadedFingerprint.matchSingle()
        val updateCall = appLoaded.instructionMatches.single()
        val activity = updateCall.getInstruction<FiveRegisterInstruction>().registerC

        appLoaded.method.addInstructions(
            updateCall.index,
            "invoke-static { v$activity }, $EXTENSION_CLASS->reportPurchasedProducts(Ljava/lang/Object;)V",
        )
    }
}
