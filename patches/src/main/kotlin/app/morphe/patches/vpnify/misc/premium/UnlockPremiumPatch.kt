/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.vpnify.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium, removes ads and the free session time limit.",
) {
    compatibleWith(AppCompatibilities.VPNIFY)
    dependsOn(spoofSignaturePatch, resourceMappingPatch)

    execute {
        SubscriptionActiveFingerprint.matchSingle().method.returnEarly(true)
        AutoRenewingSubscriptionFingerprint.matchSingle().method.returnEarly(true)

        val premiumActivatedType = PremiumActivatedTitleFingerprint.matchSingle()
            .instructionMatches.first()
            .getInstruction<ReferenceInstruction>()
            .getReference<TypeReference>()!!
            .type

        premiumActivatedCallbackFingerprint(premiumActivatedType).matchSingle().apply {
            method.removeInstruction(instructionMatches.last().index)
        }
    }
}
