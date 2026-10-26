/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blockit.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium, the big suitcase and the sound pack.",
) {
    compatibleWith(AppCompatibilities.BLOCKIT)

    dependsOn(removePairipProtectionPatch)

    execute {
        val ownedPurchases = OwnedPurchasesFingerprint.matchSingle()
        val (premiumStoreMatch, _, bigSuitcasePut, _, soundPackPut) = ownedPurchases.instructionMatches

        ownedPurchases.method.apply {
            listOf(soundPackPut.index, bigSuitcasePut.index).forEach { putIndex ->
                val ownedRegister = getInstruction<FiveRegisterInstruction>(putIndex).registerE
                addInstruction(putIndex, "const/4 v$ownedRegister, 0x1")
            }

            val premiumIndex = premiumStoreMatch.index
            val premiumStore = getInstruction<TwoRegisterInstruction>(premiumIndex)
            val premiumField = getInstruction<ReferenceInstruction>(premiumIndex).reference
            replaceInstruction(premiumIndex, "const/4 v${premiumStore.registerA}, 0x1")
            addInstruction(
                premiumIndex + 1,
                "iput-boolean v${premiumStore.registerA}, v${premiumStore.registerB}, $premiumField",
            )
        }
    }
}
