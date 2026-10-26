/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/RememberTabScrollPositionPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val LIST_STATE = "Landroidx/compose/foundation/lazy/LazyListState;"
private const val GRID_STATE = "Landroidx/compose/foundation/lazy/grid/LazyGridState;"
private const val EXTENSION = "Lapp/hxreborn/extension/firefox/ScrollMemory;"

private fun rememberStateFingerprint(stateType: String) = Fingerprint(
    definingClass = TAB_LAYOUT_KT,
    filters = listOf(
        methodCall(
            smali = "${stateType.removeSuffix(";")}Kt;->remember${stateType.substringAfterLast('/').removeSuffix(";")}" +
                "($COMPOSER)$stateType",
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

@Suppress("unused")
val rememberTabScrollPositionPatch = bytecodePatch(
    name = "Remember tab scroll position",
    description = "Reopens the tab tray at the scroll position you left it.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("scrollMemory")

        fun hookRememberedState(fingerprint: Fingerprint, stateType: String, expected: Int) {
            val matches = fingerprint.matchAll()
            if (matches.size != expected) {
                throw PatchException("Expected $expected $stateType call sites, found ${matches.size}")
            }

            matches.forEach { match ->
                val (call, result) = match.instructionMatches
                val composer = call.getInstruction<FiveRegisterInstruction>().registerC
                val state = result.getInstruction<OneRegisterInstruction>().registerA
                if (composer > 15 || state > 15) {
                    throw PatchException("Register out of range for non-range invoke: v$composer, v$state")
                }

                match.method.addInstructions(
                    result.index + 1,
                    """
                        invoke-static { v$composer, v$state }, $EXTENSION->pick(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$state
                        check-cast v$state, $stateType
                    """,
                )
            }
        }

        hookRememberedState(rememberStateFingerprint(GRID_STATE), GRID_STATE, 2)
        hookRememberedState(rememberStateFingerprint(LIST_STATE), LIST_STATE, 2)

        TabLayoutScrollHelperFingerprint.let { fingerprint ->
            val init = fingerprint.instructionMatches.single()
            val range = init.getInstruction<RegisterRangeInstruction>()

            val index = range.startRegister + 1
            val last = range.startRegister + 4
            fingerprint.method.addInstructions(
                init.index,
                """
                    invoke-static/range { v$index .. v$last }, $EXTENSION->resolveScrollIndex(IZZLjava/lang/Object;)I
                    move-result v$index
                """,
            )
        }
    }
}
