/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Gate reported by KaKi87 in https://github.com/hxreborn/morphe-patches/issues/18,
 * demonstrated for the web client in:
 * https://git.kaki87.net/KaKi87/userscripts/src/commit/b2872e081b1ac6343a6575f78df5b98cdb413004/protonmailWithoutAnnoyances/main.user.js#L45
 */
package app.morphe.patches.protonmail.misc.scheduling

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MAIL_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockCustomTimePickerPatch: Patch = patch(
    name = "Unlock custom time picker",
    description = "Enables picking a custom date and time when snoozing conversations and scheduling messages.",
) {
    dependsOn(patchesSettingsPatch)
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        markPatchApplied(MAIL_SETTINGS_CLASS, "unlockCustomTimePicker", unlockCustomTimePickerPatch)

        SnoozeOptionsFingerprint.match().apply {
            val (_, customOptionUnset, _, upgradeRequired) = instructionMatches
            val customUnset = customOptionUnset.fieldReference
            val register = upgradeRequired.getInstruction<OneRegisterInstruction>().registerA

            method.replaceInstruction(
                upgradeRequired.index,
                "sget-object v$register, $customUnset",
            )
        }

        ScheduleSendCustomOptionFingerprint.method.returnEarly(true)
    }
}
