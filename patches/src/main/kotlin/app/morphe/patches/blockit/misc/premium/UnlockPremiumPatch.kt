/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blockit.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Premium, the big suitcase and the sound pack.",
) {
    compatibleWith(AppCompatibilities.BLOCKIT)

    dependsOn(removePairipProtectionPatch)

    execute {
        val ownedPurchases = OwnedPurchasesFingerprint.match()
        val (premiumStoreMatch, _, bigSuitcasePut, _, soundPackPut) = ownedPurchases.instructionMatches

        ownedPurchases.method.apply {
            val premiumIndex = premiumStoreMatch.index
            val premiumStore = getInstruction<TwoRegisterInstruction>(premiumIndex)
            val premiumField = getInstruction<ReferenceInstruction>(premiumIndex).reference
            val ownedRegisters = listOf(bigSuitcasePut.index, soundPackPut.index)
                .map { getInstruction<FiveRegisterInstruction>(it).registerE }
            val grantedRegisters = ownedRegisters.filter { it != premiumStore.registerA }.distinct()

            replaceInstruction(premiumIndex, "const/4 v${premiumStore.registerA}, 0x1")
            addInstructions(
                premiumIndex + 1,
                grantedRegisters.joinToString("\n") { "const/4 v$it, 0x1" } +
                    "\niput-boolean v${premiumStore.registerA}, v${premiumStore.registerB}, $premiumField",
            )
        }
    }
}
