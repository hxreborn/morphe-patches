/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.layout

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.returnVoidWhen

private const val LAYOUT_COMPONENTS_CLASS = "Lapp/hxreborn/extension/protonpass/LayoutComponents;"

@Suppress("unused")
val hideLayoutComponentsPatch = bytecodePatch(
    name = "Hide layout components",
    description = "Adds settings to hide the Help center, Spread the word and About sections.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(LAYOUT_COMPONENTS_CLASS)

        HelpCenterSectionFingerprint.matchSingle().method
            .returnVoidWhen("$LAYOUT_COMPONENTS_CLASS->isHelpCenterHidden()Z")
        AboutSectionFingerprint.matchSingle().method
            .returnVoidWhen("$LAYOUT_COMPONENTS_CLASS->isAboutHidden()Z")

        SpreadTheWordSectionFingerprint.matchOrNull()?.method?.let { section ->
            section.returnVoidWhen("$LAYOUT_COMPONENTS_CLASS->isSpreadTheWordHidden()Z")
            mutableClassDefBy(LAYOUT_COMPONENTS_CLASS).methods
                .single { it.name == "hasSpreadTheWord" }
                .returnEarly(true)
        }
    }
}
