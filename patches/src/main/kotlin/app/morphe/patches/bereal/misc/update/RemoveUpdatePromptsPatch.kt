/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.setExtensionIsPatchIncluded

private val updatePromptsExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}

@Suppress("unused")
val removeUpdatePromptsPatch = bytecodePatch(
    name = "Remove update prompts",
    description = "Removes BeReal's required and suggested update popups and the Play Store update prompt.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(removePairipProtectionPatch, updatePromptsExtensionPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}UpdatePrompts;")

        listOf("androidMandatoryVersion", "androidRecommendedVersion").forEach { fieldName ->
            requiredVersionGetterFingerprint(fieldName).matchSingle().method.addInstructions(
                0,
                """
                    const/16 p0, 0x0
                    return-object p0
                """,
            )
        }

        val settingsType = InAppUpdateSettingsFingerprint.matchSingle().classDef.type
        val managerType = inAppUpdateManagerFingerprint(settingsType).matchSingle().classDef.type
        inAppUpdateEnabledFingerprint(managerType).matchSingle().method.returnEarly(false)
    }
}
