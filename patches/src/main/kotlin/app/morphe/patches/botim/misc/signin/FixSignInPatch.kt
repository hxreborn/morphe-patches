/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val fixSignInPatch = bytecodePatch(
    name = "Fix sign-in",
    description = "Fixes sending the verification code when signing in with a phone number.",
) {
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        val warmUp = IntegrityWarmUpFingerprint.matchSingle()
        val boxIndex = warmUp.instructionMatches.single().index
        val register = warmUp.method.getInstruction<FiveRegisterInstruction>(boxIndex).registerC

        warmUp.method.replaceInstruction(boxIndex, "const/4 v$register, 0x0")
        warmUp.method.addInstruction(
            boxIndex + 1,
            "invoke-static { v$register }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;",
        )
    }
}
