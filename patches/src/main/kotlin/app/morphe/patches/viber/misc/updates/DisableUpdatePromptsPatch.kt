/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableUpdatePromptsPatch: BytecodePatch = bytecodePatch(
    name = "Disable update prompts",
    description = "Stops the optional Google Play update dialog. Required updates still show.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("disableUpdatePrompts", disableUpdatePromptsPatch)
        val configType = UpdatePromptConfigFingerprint.matchSingle().classDef.type
        updatePromptCheckFingerprint(configType).matchSingle().apply {
            val (timedCheck, forcedCheck, _) = instructionMatches
            listOf(forcedCheck, timedCheck).forEach {
                val register = (it.instruction as OneRegisterInstruction).registerA
                method.addInstructions(
                    it.index + 1,
                    """
                        invoke-static/range { v$register .. v$register }, $VIBER_SETTINGS_CLASS->allowUpdatePrompt(Z)Z
                        move-result v$register
                    """,
                )
            }
        }
    }
}
