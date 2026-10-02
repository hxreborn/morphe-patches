/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal open class FreeServerCheckFingerprint(
    parameters: List<String>,
) : Fingerprint(
    returnType = "Z",
    parameters = parameters,
    filters = listOf(
        methodCall(definingClass = "Lcom/protonvpn/android/servers/Server;", name = "isFreeServer"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal fun BytecodePatchContext.invertFreeServerCheckForFreeAccount(check: FreeServerCheckFingerprint) {
    check.matchSingle().run {
        val isFreeServerResult = instructionMatches.last()
        val register = isFreeServerResult.getInstruction<OneRegisterInstruction>().registerA
        method.addInstructions(
            isFreeServerResult.index + 1,
            """
                invoke-static { v$register }, Lapp/hxreborn/extension/protonvpn/FreeServerLocations;->shouldExcludeServer(Z)Z
                move-result v$register
            """,
        )
    }
}
