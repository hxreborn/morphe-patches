/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.monet.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Monet Premium.",
) {
    compatibleWith(AppCompatibilities.MONET)

    execute {
        val billingManager = BillingManagerConstructorFingerprint.matchSingle()
        val stateType = billingManager.method
            .getInstruction(billingManager.instructionMatches.single().index)
            .getReference<MethodReference>()!!
            .definingClass

        val stateConstructor = premiumStateConstructorFingerprint(stateType).matchSingle()
        val isPremiumName = stateConstructor.method
            .getInstruction(stateConstructor.instructionMatches.single().index)
            .getReference<MethodReference>()!!
            .name

        isPremiumFingerprint(stateType, isPremiumName).matchSingle().method.returnEarly(true)
    }
}
