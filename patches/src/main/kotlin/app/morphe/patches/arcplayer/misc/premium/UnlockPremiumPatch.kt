/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.arcplayer.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the pure black theme and custom subtitle fonts, and removes the premium prompts.",
) {
    compatibleWith(AppCompatibilities.ARC_PLAYER)

    execute {
        val premiumStateInit = PremiumStateInitFingerprint.matchSingle()
        val premiumWrite = premiumStateInit.instructionMatches.last()
        val premiumRegister = premiumWrite.getInstruction<OneRegisterInstruction>().registerA

        premiumStateInit.method.addInstruction(premiumWrite.index, "const/4 v$premiumRegister, 0x1")
    }
}
