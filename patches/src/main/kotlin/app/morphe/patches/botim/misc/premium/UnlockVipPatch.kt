/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.botim.misc.signin.fixSignInPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockVipPatch = bytecodePatch(
    name = "Unlock VIP",
    description = "Removes ads and upgrade prompts. Shows the VIP badge in the header.",
) {
    compatibleWith(AppCompatibilities.BOTIM)

    dependsOn(fixSignInPatch)

    execute {
        val mapper = SelfProfileMapperFingerprint.matchSingle()
        val moveResultIndex = mapper.instructionMatches.single().index + 1
        val register = mapper.method.getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

        mapper.method.addInstruction(moveResultIndex + 1, "const/4 v$register, 0x1")
    }
}
