/*
 * Copyright (C) 2026 kiraio-moe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from kiraio-moe/Lain-Patches:
 * https://github.com/kiraio-moe/Lain-Patches/commit/6e972994f9631e46869029eb43e96f2cc206dfac
 * Commit 6e972994f9631e46869029eb43e96f2cc206dfac (2026-07-09),
 * patches/src/main/kotlin/kiraio/lain/ilovepdf/premium/PremiumPatch.kt
 */
package app.morphe.patches.ilovepdf.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium PDF tools and cloud storage features.",
) {
    compatibleWith(AppCompatibilities.ILOVEPDF)

    execute {
        PremiumBooleanFingerprint.matchAll(2..2).forEach { match ->
            val (premiumWrite) = match.instructionMatches
            val register = (premiumWrite.instruction as TwoRegisterInstruction).registerA
            match.method.addInstruction(premiumWrite.index, "const/4 v$register, 0x1")
        }
        PremiumBoxedBooleanFingerprint.matchAll(3..3).forEach { match ->
            val (premiumWrite) = match.instructionMatches
            val register = (premiumWrite.instruction as TwoRegisterInstruction).registerA
            match.method.addInstruction(
                premiumWrite.index,
                "sget-object v$register, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;",
            )
        }
    }
}
