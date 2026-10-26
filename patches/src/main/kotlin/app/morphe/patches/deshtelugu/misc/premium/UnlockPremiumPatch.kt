/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.deshtelugu.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Premium, which removes promoted app suggestions and the Desh Keyboard " +
        "signature added to messages and shared stickers.",
) {
    compatibleWith(AppCompatibilities.DESH_TELUGU_KEYBOARD)

    execute {
        val purchaseQuery = PremiumPurchaseQueryFingerprint.matchSingle()
        val (_, ownedFlowValue, _, ownedPreferenceWrite) = purchaseQuery.instructionMatches

        val preferenceValueRegister = ownedPreferenceWrite.getInstruction<FiveRegisterInstruction>().registerE
        purchaseQuery.method.addInstruction(ownedPreferenceWrite.index, "const/4 v$preferenceValueRegister, 0x1")

        val flowValueRegister = ownedFlowValue.getInstruction<FiveRegisterInstruction>().registerC
        purchaseQuery.method.addInstruction(ownedFlowValue.index, "const/4 v$flowValueRegister, 0x1")
    }
}
