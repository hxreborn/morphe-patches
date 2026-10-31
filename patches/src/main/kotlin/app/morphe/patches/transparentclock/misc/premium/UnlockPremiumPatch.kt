/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.transparentclock.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.smali.parameterRegister
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium features and removes ads. " +
        "Premium themes and widgets downloaded from Google Play are not included.",
) {
    compatibleWith(AppCompatibilities.TRANSPARENT_CLOCK)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        EntitlementCheckFingerprint.matchAll(2..2).forEach { it.method.returnEarly(true) }

        val adFreeRead = AdFreePreferenceReadFingerprint.matchSingle()
        val adFreeStore = adFreeRead.instructionMatches.last()
        val adFreeField = adFreeStore.getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!
        val adFreeRegister = adFreeStore.getInstruction<TwoRegisterInstruction>().registerA
        adFreeRead.method.addInstruction(adFreeStore.index, "const v$adFreeRegister, 0x1")

        adFreeSetterFingerprint(adFreeField.definingClass).matchSingle().method.apply {
            addInstruction(0, "const ${parameterRegister(parameterTypes.lastIndex)}, 0x1")
        }
    }
}
