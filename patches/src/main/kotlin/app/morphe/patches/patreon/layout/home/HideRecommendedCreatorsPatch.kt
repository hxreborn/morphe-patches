/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/patreon/HomeFeedFilter;"

@Suppress("unused")
val hideRecommendedCreatorsPatch = bytecodePatch(
    name = "Hide recommended creators",
    description = "Limits Home to creators you're a member of.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(EXTENSION_CLASS_DESCRIPTOR)

        LauncherFeedResponseConstructorFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->removeExploreSections(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """,
        )

        listOf("LauncherFeedSectionSchema", "FeedsV2LauncherFeedSectionSchema").forEach { sectionSchema ->
            val sectionItems = launcherSectionItemsFingerprint(sectionSchema)
            val returnIndex = sectionItems.instructionMatches.single().index
            sectionItems.method.apply {
                val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
                addInstructions(
                    returnIndex,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->removeNonMemberItems(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                    """,
                )
            }
        }
    }
}
