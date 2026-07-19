/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val hideSidebarUpsellingPatch = bytecodePatch(
    name = "Hide sidebar upselling",
    description = "Hides the 'Upgrade to Mail Plus' entry from the sidebar.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        SidebarUpsellingFeatureFlagFingerprint.method.apply {
            val flagIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()?.string == "MailAndroidUpsellingSidebar"
            }
            val resultIndex = indexOfFirstInstructionOrThrow(flagIndex, Opcode.MOVE_RESULT)
            val register = getInstruction<OneRegisterInstruction>(resultIndex).registerA

            replaceInstruction(resultIndex, "const/16 v$register, 0x0")
        }
    }
}
