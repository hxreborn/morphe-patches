/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.hypic.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val TOP_VIP_STATE = "VIP4"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro filters, effects, templates and tools. " +
        "AI features that use server credits are not unlocked.",
) {
    compatibleWith(AppCompatibilities.HYPIC)

    dependsOn(spoofSignaturePatch)

    execute {
        AccountVipCheckFingerprint.method.returnEarly(true)

        VipStateUpdateFingerprint.match().let { match ->
            val (stateWrite) = match.instructionMatches
            val stateRegister = stateWrite.getInstruction<TwoRegisterInstruction>().registerA
            match.method.addInstruction(
                stateWrite.index,
                "sget-object v$stateRegister, $VIP_STATE_CLASS->$TOP_VIP_STATE:$VIP_STATE_CLASS",
            )
        }
    }
}
