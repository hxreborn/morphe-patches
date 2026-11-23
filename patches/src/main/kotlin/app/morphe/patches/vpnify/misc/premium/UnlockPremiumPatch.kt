/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.vpnify.misc.fix.signature.spoofSignaturePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium, removes ads and the free session time limit.",
) {
    compatibleWith(AppCompatibilities.VPNIFY)
    dependsOn(spoofSignaturePatch)

    execute {
        SubscriptionActiveFingerprint.method.returnEarly(true)
        AutoRenewingSubscriptionFingerprint.method.returnEarly(true)

        val premiumActivatedType = PremiumActivatedTitleFingerprint.match()
            .instructionMatches.first()
            .getInstruction<ReferenceInstruction>()
            .getReference<TypeReference>()!!
            .type

        premiumActivatedCallbackFingerprint(premiumActivatedType).match().apply {
            method.removeInstruction(instructionMatches.last().index)
        }
    }
}
