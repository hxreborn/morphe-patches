/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied

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
            val (timedCheck, updateLinkCheck, _) = instructionMatches
            listOf(updateLinkCheck, timedCheck).forEach {
                method.addFilterCallAfter(it.index, "$VIBER_SETTINGS_CLASS->allowUpdatePrompt(Z)Z")
            }
        }
    }
}
