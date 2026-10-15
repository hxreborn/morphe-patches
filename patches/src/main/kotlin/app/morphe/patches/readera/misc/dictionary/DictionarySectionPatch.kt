/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.dictionary

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val dictionarySectionPatch = bytecodePatch(
    name = "Dictionary section",
    description = "Adds the Dictionary section with all saved words to the main menu.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        DrawerMenuVisibilityFingerprint.let {
            val hideIndex = it.instructionMatches[4].index
            it.method.apply {
                val itemRegister = getInstruction<FiveRegisterInstruction>(hideIndex).registerC
                val visibleRegister = getFreeRegisterProvider(hideIndex, 1, itemRegister).getFreeRegister4Bit()
                replaceInstruction(
                    hideIndex,
                    "invoke-interface { v$itemRegister, v$visibleRegister }, Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;",
                )
                addInstruction(hideIndex, "const/4 v$visibleRegister, 0x1")
            }
        }
    }
}
