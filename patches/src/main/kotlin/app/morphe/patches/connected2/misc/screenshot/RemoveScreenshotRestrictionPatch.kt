/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.screenshot

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

private val TIMED_MEDIA_EXTRAS = listOf("media_gallery_is_timed", "video_play_is_timed")

@Suppress("unused")
val removeScreenshotRestrictionPatch = bytecodePatch(
    name = "Remove screenshot restriction",
    description = "Allows screenshots of view-once photos and videos.",
) {
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        TIMED_MEDIA_EXTRAS.forEach { extra ->
            timedMediaOnCreateFingerprint(extra).matchSingle().apply {
                method.removeInstruction(instructionMatches.last().index)
            }
        }
    }
}
