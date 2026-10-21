/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.shared

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val CONNECTIONS_MANAGER_CLASS = "Lorg/telegram/tgnet/ConnectionsManager;"

internal val licenseCheck = methodCall(definingClass = CONNECTIONS_MANAGER_CLASS, name = "native_checkLicense")
internal val licenseDaysLeft = methodCall(definingClass = CONNECTIONS_MANAGER_CLASS, name = "native_daysFile")
internal val licenseExpired = methodCall(definingClass = CONNECTIONS_MANAGER_CLASS, name = "native_expireFile")

internal fun MutableMethod.overrideResultsOf(call: InstructionFilter, value: Int) {
    findInstructionIndicesReversed(call).forEach { callIndex ->
        val moveResult = getInstruction<OneRegisterInstruction>(callIndex + 1)
        if (moveResult.opcode != Opcode.MOVE_RESULT) {
            throw PatchException("Native license call in $this is not followed by move-result")
        }
        addInstruction(callIndex + 2, "const/16 v${moveResult.registerA}, $value")
    }
}
