/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.shared

import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.addFreeRegisters
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PREFERENCE_CALL_WINDOW = 1
private const val SAVED_REGISTERS = 2

internal fun preferenceFilters(key: String) = listOf(
    string(key),
    methodCall(
        parameters = listOf("Ljava/lang/String;", "Z"),
        location = MatchAfterWithin(PREFERENCE_CALL_WINDOW),
    ),
)

context(_: BytecodePatchContext)
internal fun List<Match>.redirectPreferences(keyOverride: String, defaultOverride: String) {
    groupBy { it.method }.forEach { (method, matches) ->
        val savedKey = method.implementation!!.registerCount
        val savedDefault = savedKey + 1
        val (expanded, parameterMoves) = method.addFreeRegisters(SAVED_REGISTERS)
        matches.sortedByDescending { match ->
            val (_, preferenceCall) = match.instructionMatches
            preferenceCall.index
        }.forEach { match ->
            val (keyRead, preferenceCall) = match.instructionMatches
            val keyRegister = (keyRead.instruction as OneRegisterInstruction).registerA
            val call = preferenceCall.instruction as FiveRegisterInstruction
            if (call.registerD != keyRegister) {
                throw PatchException("The preference call in ${match.originalMethod} does not take the matched key")
            }
            val defaultRegister = call.registerE
            val callIndex = preferenceCall.index + parameterMoves
            val result = expanded.getInstruction(callIndex + 1)
            val resultRegisters = when (result.opcode) {
                Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> setOf((result as OneRegisterInstruction).registerA)
                Opcode.MOVE_RESULT_WIDE -> (result as OneRegisterInstruction).registerA.let { setOf(it, it + 1) }
                else -> emptySet()
            }
            val restores = listOfNotNull(
                "move-object/from16 v$keyRegister, v$savedKey".takeIf { keyRegister !in resultRegisters },
                "move/from16 v$defaultRegister, v$savedDefault".takeIf { defaultRegister !in resultRegisters },
            )
            if (restores.isNotEmpty()) {
                expanded.addInstructions(callIndex + if (resultRegisters.isEmpty()) 1 else 2, restores.joinToString("\n"))
            }
            expanded.addInstructions(
                callIndex,
                """
                    move-object/16 v$savedKey, v$keyRegister
                    move/16 v$savedDefault, v$defaultRegister
                    invoke-static { v$keyRegister }, $keyOverride
                    move-result-object v$keyRegister
                    invoke-static { v$defaultRegister }, $defaultOverride
                    move-result v$defaultRegister
                """,
            )
        }
    }
}
