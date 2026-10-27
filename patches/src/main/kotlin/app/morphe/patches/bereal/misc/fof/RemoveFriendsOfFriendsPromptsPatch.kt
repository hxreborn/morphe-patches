/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.fof

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.returnEarlyWhen
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}fof/FriendsOfFriendsPrompts;"

@Suppress("unused")
val removeFriendsOfFriendsPromptsPatch = bytecodePatch(
    name = "Remove Friends of Friends prompts",
    description = "Removes the \"Share with Friends of Friends\" card and the end-of-feed messages.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val footerType = EndOfFeedFooterToStringFingerprint.matchSingle().classDef.type
        val footer = endOfFeedFooterVisibilityFingerprint(footerType).matchSingle()
        val flagIndex = footer.instructionMatch(Opcode.MOVE_RESULT).index
        val flag = footer.method.getInstruction<OneRegisterInstruction>(flagIndex).registerA
        footer.method.addInstructions(
            flagIndex + 1,
            """
                invoke-static/range { v$flag .. v$flag }, $EXTENSION_CLASS->showFooter(Z)Z
                move-result v$flag
            """,
        )

        val notActivatedType = NotActivatedToStringFingerprint.matchSingle().classDef.type
        activationCardFingerprint(notActivatedType).matchSingle().method
            .returnEarlyWhen("$EXTENSION_CLASS->removed()Z")
    }
}
