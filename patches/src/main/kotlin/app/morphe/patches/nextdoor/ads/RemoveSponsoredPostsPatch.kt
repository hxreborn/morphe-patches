/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.ads

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction

@Suppress("unused")
val removeSponsoredPostsPatch = bytecodePatch(
    name = "Remove sponsored posts",
    description = "Removes sponsored posts from the feed.",
) {
    compatibleWith(AppCompatibilities.NEXTDOOR)

    execute {
        val match = AdsDataItemConverterFingerprint.matchSingle()
        fun indexOf(filter: InstructionFilter) = match.instructionMatches.single { it.filter === filter }.index

        match.method.apply {
            val returnWithoutRows =
                getInstruction<BuilderOffsetInstruction>(indexOf(missingAdBranch)).target.location.instruction!!
            listOf(adTypeBranch, adPlaceholderTypeBranch).map(::indexOf).sortedDescending().forEach { branchIndex ->
                addInstructionsWithLabels(
                    branchIndex + 1,
                    "goto :return_without_rows",
                    ExternalLabel("return_without_rows", returnWithoutRows),
                )
            }
        }
    }
}
