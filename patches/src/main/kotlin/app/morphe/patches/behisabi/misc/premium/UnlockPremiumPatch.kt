/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.behisabi.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks BeHisabi Pro features. AI features require your own API key.",
) {
    compatibleWith(AppCompatibilities.BEHISABI)
    dependsOn(removePairipProtectionPatch)

    execute {
        val (_, lifetimeWrite) = LifetimeTierFingerprint.matchSingle().instructionMatches
        val lifetime = lifetimeWrite.instruction.getReference<FieldReference>()!!
        val proState = publishProStateFingerprint(lifetime.type).matchSingle()
        val (_, playProRead, playTierRead) = proState.instructionMatches
        val playPro = playProRead.instruction.getReference<FieldReference>()!!
        val playTier = playTierRead.instruction.getReference<FieldReference>()!!
        val proRegister = proState.method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        proState.method.addInstructions(
            0,
            """
                const/4 v$proRegister, 0x1
                iput-boolean v$proRegister, p0, $playPro
                sget-object v$proRegister, $lifetime
                iput-object v$proRegister, p0, $playTier
            """,
        )
    }
}
