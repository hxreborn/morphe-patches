/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.cloneParameters
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/patreon/HomeFeedFilter;"

@Suppress("unused")
val hideRecommendedCreatorsPatch: Patch = patch(
    name = "Hide recommended creators",
    description = "Limits Home and the posts under a post to creators you're a member of.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(EXTENSION_CLASS_DESCRIPTOR, hideRecommendedCreatorsPatch)
        replaceHomeFeedStubs(EXTENSION_CLASS_DESCRIPTOR)

        LauncherFeedResponseConstructorFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->removeExploreSections(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """,
        )

        listOf("LauncherFeedSectionSchema", "FeedsV2LauncherFeedSectionSchema").forEach { sectionSchema ->
            val sectionItems = launcherSectionItemsFingerprint(sectionSchema)
            val getter = sectionItems.method
            val getterWithSection = getter.cloneParameters()
            val returnIndex = sectionItems.instructionMatches.single().index +
                getterWithSection.instructions.size - getter.instructions.size
            getterWithSection.apply {
                val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
                addInstructions(
                    returnIndex,
                    """
                        invoke-static { p0, v$register }, $EXTENSION_CLASS_DESCRIPTOR->removeNonMemberItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                    """,
                )
            }
        }

        RecommendedPostsPageItemsFingerprint.let { pageItems ->
            val returnIndex = pageItems.instructionMatches.single().index
            pageItems.method.apply {
                val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
                addInstructions(
                    returnIndex,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->removePostViewerItems(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                    """,
                )
            }
        }

        FeedSectionItemStatesFingerprint.let { itemStates ->
            val (sectionCall, itemCall, stateCast) = itemStates.instructionMatches
            itemStates.method.apply {
                val section = getInstruction<OneRegisterInstruction>(sectionCall.index + 1).registerA
                val item = getInstruction<OneRegisterInstruction>(itemCall.index + 1).registerA
                val state = stateCast.getInstruction<OneRegisterInstruction>().registerA
                addInstructions(
                    stateCast.index + 1,
                    """
                        invoke-static { v$state, v$item, v$section }, $EXTENSION_CLASS_DESCRIPTOR->removeNonMemberItemState(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$state
                        check-cast v$state, Lcom/patreon/android/data/model/DataResult;
                    """,
                )
            }
        }

        RecommendedPostsQueryPostIdFingerprint.let { postId ->
            val returnIndex = postId.instructionMatches.single().index
            postId.method.apply {
                val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA
                addInstructions(
                    returnIndex,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->recordViewedPost(Ljava/lang/Object;)V",
                )
            }
        }
    }
}
