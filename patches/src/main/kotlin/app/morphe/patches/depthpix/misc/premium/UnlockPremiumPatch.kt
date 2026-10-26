/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro wallpapers, 3D motion, particles and clock presets. " +
        "Unlimited photo designs are not included.",
) {
    compatibleWith(AppCompatibilities.DEPTHPIX)

    dependsOn(removePairipProtectionPatch)

    execute {
        UserToStringFingerprint.matchSingle().classDef.apply {
            val subscribedField = fields.single { it.type == "Z" }
            val lifetimeField = fields.single { it.type == "Ljava/lang/Boolean;" }

            methods.filter { it.name == "<init>" }.forEach { constructor ->
                constructor.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                    constructor.replaceInstruction(index, "const/4 p1, 0x1")
                    constructor.addInstructions(
                        index + 1,
                        """
                            iput-boolean p1, p0, $subscribedField
                            sget-object p1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                            iput-object p1, p0, $lifetimeField
                            return-void
                        """,
                    )
                }
            }
        }
    }
}
