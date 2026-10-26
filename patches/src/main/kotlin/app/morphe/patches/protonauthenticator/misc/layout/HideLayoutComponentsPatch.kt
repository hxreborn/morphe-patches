/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.returnVoidWhen
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SETTINGS_SECTIONS_CLASS = "Lapp/hxreborn/extension/protonauthenticator/SettingsSections;"
private const val NEXT_INSTRUCTION = 1

@Suppress("unused")
val hideLayoutComponentsPatch = bytecodePatch(
    name = "Hide layout components",
    description = "Adds settings to hide the Spread the word section, the Discover Proton section " +
        "and the Proton Pass banner.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(SETTINGS_SECTIONS_CLASS)

        PassBannerFingerprint.matchSingle().method
            .returnVoidWhen("$SETTINGS_SECTIONS_CLASS->isPassBannerHidden()Z")

        DiscoverSectionVisibilityFingerprint.matchSingle().method.apply {
            val free = getFreeRegisterProvider(0, 1).getFreeRegister()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $SETTINGS_SECTIONS_CLASS->isDiscoverProtonHidden()Z
                    move-result v$free
                    if-eqz v$free, :show
                    const/4 v$free, 0x0
                    return v$free
                """,
                ExternalLabel("show", getInstruction(0)),
            )
        }

        SettingsSectionFingerprint.matchSingle().method.apply {
            val free = getFreeRegisterProvider(0, 1).getFreeRegister()

            addInstructionsWithLabels(
                0,
                """
                    invoke-interface/range { p1 .. p1 }, Ljava/util/List;->isEmpty()Z
                    move-result v$free
                    if-eqz v$free, :show
                    return-void
                """,
                ExternalLabel("show", getInstruction(0)),
            )
        }

        SpreadTheWordRowsFingerprint.matchSingle().apply {
            val rows = instructionMatches.last().getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                instructionMatches.last().index + NEXT_INSTRUCTION,
                """
                    invoke-static/range { v$rows .. v$rows }, $SETTINGS_SECTIONS_CLASS->spreadTheWordRows(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$rows
                """,
            )
        }
    }
}
