/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.protonvpn.misc.anchors.isFreeUserCall
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal fun BytecodePatchContext.freeUserCheckFingerprint(
    accessFlags: List<AccessFlags>? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    strings: List<String>? = null,
    vararg followingFilters: InstructionFilter,
) = Fingerprint(
    accessFlags = accessFlags,
    returnType = returnType,
    parameters = parameters,
    strings = strings,
    filters = listOf(
        isFreeUserCall(),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ) + followingFilters,
)

internal fun BytecodePatchContext.clearFreeUserCheck(check: Fingerprint) {
    check.matchSingle().run {
        val freeUserResult = instructionMatches[1]
        val register = freeUserResult.getInstruction<OneRegisterInstruction>().registerA
        method.addInstruction(freeUserResult.index + 1, "const/16 v$register, 0x0")
    }
}
