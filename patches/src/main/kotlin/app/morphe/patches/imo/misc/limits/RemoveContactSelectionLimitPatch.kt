/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val CONTACT_LIMIT = 999

@Suppress("unused")
val removeContactSelectionLimitPatch = bytecodePatch(
    name = "Remove contact selection limit",
    description = "Raises the limit on contacts selected for iBubble close friends and Family Guard invites to 999. " +
        "Experimental.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        CloseFriendLimitFingerprint.matchSingle().method.apply {
            val limitIndex = indexOfFirstLiteralInstructionOrThrow(STOCK_CLOSE_FRIEND_LIMIT)
            val register = getInstruction<OneRegisterInstruction>(limitIndex).registerA
            replaceInstruction(limitIndex, "const/16 v$register, $CONTACT_LIMIT")
        }
        CloseFriendPreselectionFingerprint.matchSingle().method.apply {
            val countIndex = indexOfFirstInstructionOrThrow(preselectionCountCall)
            val limitRegister = getInstruction<FiveRegisterInstruction>(countIndex).registerC
            addInstruction(countIndex, "const/16 v$limitRegister, $STOCK_CLOSE_FRIEND_LIMIT")
        }
        FamilyGuardContactLimitFingerprint.matchSingle().method.returnEarly(CONTACT_LIMIT)
    }
}
