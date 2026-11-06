/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.prompts

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.patreon.misc.featureflags.featureFlagsPatch
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val APP_PROMPTS_CLASS = "${PATREON_EXTENSION_PACKAGE}AppPrompts;"

@Suppress("unused")
val removeAppPromptsPatch: BytecodePatch = bytecodePatch(
    name = "Remove app prompts",
    description = "Removes the rating prompt and the in-app update prompt.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch, featureFlagsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(APP_PROMPTS_CLASS, removeAppPromptsPatch)
    }
}
