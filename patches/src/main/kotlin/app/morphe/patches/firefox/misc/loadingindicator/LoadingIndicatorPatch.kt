/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hxreborn/pixel-nits:
 * app/src/main/kotlin/eu/hxreborn/pixelinjector/xposed/hook/firefox/FirefoxLoadingIndicator.kt
 */
package app.morphe.patches.firefox.misc.loadingindicator

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION = "Lapp/hxreborn/extension/firefox/LoadingIndicator;"

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "M3E loading indicator",
    description = "Replaces the pull-to-refresh spinner with the Material 3 Expressive " +
        "shape-morphing loading indicator.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("loadingIndicator")

        val match = SwipeRefreshLayoutConstructorFingerprint.matchSingle()
        match.method.apply {
            val (_, diameter) = match.instructionMatches
            val diameterIndex = diameter.index
            val diameterRegister = getInstruction<OneRegisterInstruction>(diameterIndex).registerA
            addInstructions(
                diameterIndex + 1,
                """
                    invoke-static/range { v$diameterRegister .. v$diameterRegister }, $EXTENSION->circleDiameter(F)F
                    move-result v$diameterRegister
                """,
            )
            addInstruction(
                indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_VOID),
                "invoke-static { p0 }, $EXTENSION->install(Landroid/view/ViewGroup;)V",
            )
        }
    }
}
