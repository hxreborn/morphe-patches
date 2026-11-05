/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.quips

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.layout.home.launcherSectionItemsFingerprint
import app.morphe.patches.patreon.misc.featureflags.featureFlagsPatch
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val QUIPS_AND_AUTOPLAY_CLASS = "${PATREON_EXTENSION_PACKAGE}QuipsAndAutoplay;"
private const val HOME_FEED_FILTER_CLASS = "${PATREON_EXTENSION_PACKAGE}HomeFeedFilter;"

@Suppress("unused")
val disableQuipsAndAutoplayPatch: BytecodePatch = bytecodePatch(
    name = "Disable Quips and autoplay",
    description = "Removes Quip cards and the Quip button, and stops feed videos from autoplaying or preloading.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch, featureFlagsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(QUIPS_AND_AUTOPLAY_CLASS, disableQuipsAndAutoplayPatch)

        val sectionItems = launcherSectionItemsFingerprint("LauncherFeedSectionSchema")
        val returnIndex = sectionItems.instructionMatches.single().index
        sectionItems.method.apply {
            val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            addInstructions(
                returnIndex,
                """
                    invoke-static { v$register }, $HOME_FEED_FILTER_CLASS->removeQuipCards(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$register
                """,
            )
        }

        AutoPlaybackSettingParserFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $QUIPS_AND_AUTOPLAY_CLASS->isDisabled()Z
                    move-result v0
                    if-eqz v0, :keep_setting
                    sget-object v0, $AUTO_PLAYBACK_SETTING->Off:$AUTO_PLAYBACK_SETTING
                    return-object v0
                """,
                ExternalLabel("keep_setting", getInstruction(0)),
            )
        }
    }
}
