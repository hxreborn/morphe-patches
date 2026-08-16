/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Gate reported by KaKi87 in https://github.com/hxreborn/morphe-patches/issues/18,
 * demonstrated for the web client in:
 * https://git.kaki87.net/KaKi87/userscripts/src/commit/b2872e081b1ac6343a6575f78df5b98cdb413004/protonmailWithoutAnnoyances/main.user.js#L45
 */
package app.morphe.patches.protonmail.misc.scheduling

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val unlockCustomTimePickerPatch = bytecodePatch(
    name = "Unlock custom time picker",
    description = "Enables picking a custom date and time when snoozing conversations and scheduling messages.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        SnoozeOptionsFingerprint.instructionMatches.let { matches ->
            val customUnset = matches[1].getInstruction<ReferenceInstruction>().reference
            val upgradeRequired = matches[3]
            val register = upgradeRequired.getInstruction<OneRegisterInstruction>().registerA

            SnoozeOptionsFingerprint.method.replaceInstruction(
                upgradeRequired.index,
                "sget-object v$register, $customUnset",
            )
        }

        ScheduleSendCustomOptionFingerprint.method.returnEarly(true)
    }
}
