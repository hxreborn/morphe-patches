/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.base.beRealBasePatch
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.berealUpdatePromptsExtensionPatch

@Suppress("unused")
val removeUpdatePromptsPatch: Patch = patch(
    name = "Remove update prompts",
    description = "Removes BeReal's required and suggested update dialogs and the Play Store update prompt.",
) {
    dependsOn(berealUpdatePromptsExtensionPatch)
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(beRealBasePatch)

    execute {
        markFeaturePatched("${BEREAL_EXTENSION_PACKAGE}UpdatePrompts;", removeUpdatePromptsPatch)

        listOf("androidMandatoryVersion", "androidRecommendedVersion").forEach { fieldName ->
            requiredVersionGetterFingerprint(fieldName).method.addInstructions(
                0,
                """
                    const/16 p0, 0x0
                    return-object p0
                """,
            )
        }

        val settingsType = InAppUpdateSettingsFingerprint.match().classDef.type
        val managerType = inAppUpdateManagerFingerprint(settingsType).match().classDef.type
        inAppUpdateEnabledFingerprint(managerType).method.returnEarly(false)
    }
}
