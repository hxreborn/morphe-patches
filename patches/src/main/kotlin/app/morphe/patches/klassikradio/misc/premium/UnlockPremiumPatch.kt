/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.klassikradio.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the premium music channels, on-demand playback and track skipping. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.KLASSIK_RADIO)

    execute {
        val subscriptionType = SubscriptionTypeFingerprint.matchSingle()
        val premium = subscriptionType.instructionMatches.last()
            .getInstruction<ReferenceInstruction>()
            .getReference<FieldReference>()!!

        contractConstructorFingerprint(subscriptionType.classDef.type).matchSingle().apply {
            val index = instructionMatches.first().index
            val register = method.getInstruction<TwoRegisterInstruction>(index).registerA

            method.addInstruction(index, "sget-object v$register, $premium")
        }
    }
}
