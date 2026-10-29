/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.launch

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.clipstudiopaint.misc.PatchSite
import app.morphe.patches.clipstudiopaint.misc.WORD_SIZE
import app.morphe.patches.clipstudiopaint.misc.rewriteNativeLibrary
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import java.nio.ByteBuffer

private const val ADRP_X9_APP_CONTEXT_PAGE = 0xb0003529u
private const val LDR_X9_APP_CONTEXT = 0xf9400529u
private const val LDR_X9_LAUNCH_SETTINGS = 0xf9401d29u
private const val MOV_W10_DOODLE_MODE_CHOICE = 0x5280004a
private const val STR_W10_LAUNCH_CHOICE = 0xb900212au
private const val B_EXIT_STATE_FACTORY = 0x14000eab

private val chooseDoodleModeAndExit = intArrayOf(
    ADRP_X9_APP_CONTEXT_PAGE.toInt(),
    LDR_X9_APP_CONTEXT.toInt(),
    LDR_X9_LAUNCH_SETTINGS.toInt(),
    MOV_W10_DOODLE_MODE_CHOICE,
    STR_W10_LAUNCH_CHOICE.toInt(),
    B_EXIT_STATE_FACTORY,
)

private val replacePlanSelectionState: (ByteBuffer, Int) -> Unit = { buffer, start ->
    chooseDoodleModeAndExit.forEachIndexed { index, word -> buffer.putInt(start + index * WORD_SIZE, word) }
}

private val patchSites = listOf(
    PatchSite(
        intArrayOf(
            0xd10283ffu.toInt(),
            0xa9047bfdu.toInt(),
            0xf9002bfbu.toInt(),
            0xa90667fau.toInt(),
            0xa9075ff8u.toInt(),
            0xa90857f6u.toInt(),
            0xa9094ff4u.toInt(),
            0x910103fdu.toInt(),
            0xd53bd058u.toInt(),
            0xaa0803f3u.toInt(),
            0xf9401708u.toInt(),
            0xaa0003f5u.toInt(),
        ),
        replacePlanSelectionState,
    ),
)

@Suppress("unused")
val skipPlanSelectionPatch = rawResourcePatch(
    name = "Skip plan selection",
    description = "Opens the app in doodle mode without showing the plan selection screen. " +
        "Log in from the home screen to use a plan.",
) {
    compatibleWith(AppCompatibilities.CLIP_STUDIO_PAINT)
    availability(requireArm64)

    execute {
        rewriteNativeLibrary(patchSites)
    }
}
