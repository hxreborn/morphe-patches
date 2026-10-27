/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.typing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}typing/TypingIndicator;"

@Suppress("unused")
val disableTypingIndicatorPatch = bytecodePatch(
    name = "Disable typing indicator",
    description = "Stops showing friends that you're typing in a chat. You still see when they type.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val match = SendTypingStateFingerprint.matchSingle()
        val moveResult = match.instructionMatch(Opcode.MOVE_RESULT).index
        val register = match.method.getInstruction<OneRegisterInstruction>(moveResult).registerA
        match.method.addInstructions(
            moveResult + 1,
            """
                invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->shouldSend(Z)Z
                move-result v$register
            """,
        )
    }
}
