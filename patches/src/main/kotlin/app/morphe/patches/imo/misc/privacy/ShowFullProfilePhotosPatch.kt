/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnBoxedBooleanEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val showFullProfilePhotosPatch = bytecodePatch(
    name = "Show full profile photos",
    description = "Opens profile photos in full size when their owner restricts it.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        ProfileAvatarFlagsFingerprint.matchSingle().method.apply {
            avatarPrivacyKeys.forEach { key ->
                val readIndex = indexOfFirstInstructionOrThrow(indexOfFirstStringInstructionOrThrow(key), jsonFlagRead)
                val flagIndex = indexOfFirstInstructionOrThrow(readIndex, Opcode.MOVE_RESULT)
                val register = getInstruction<OneRegisterInstruction>(flagIndex).registerA
                addInstruction(flagIndex + 1, "const/4 v$register, 0x1")
            }
        }

        VoiceRoomFullAvatarFingerprint.matchSingle().method.returnBoxedBooleanEarly(true)
    }
}
