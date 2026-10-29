/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.clipstudiopaint.misc.PatchSite
import app.morphe.patches.clipstudiopaint.misc.WORD_SIZE
import app.morphe.patches.clipstudiopaint.misc.rewriteNativeLibrary
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import java.nio.ByteBuffer

private const val ZERO_REGISTER = 0x1f

private const val ADRP_X8_REDIRECT_PAGE = 0xf0025468u
private const val ADD_X8_REDIRECT_OFFSET = 0x912fc108u

private const val MOV_W0_ALL_PRODUCT_PLAN = 0x52800040
private const val RET = 0xd65f03c0u

private val clearDoodleFlagStore: (ByteBuffer, Int) -> Unit = { buffer, start ->
    val store = start + WORD_SIZE
    buffer.putInt(store, buffer.getInt(store) or ZERO_REGISTER)
}

private val redirectDoodleFlagStore: (ByteBuffer, Int) -> Unit = { buffer, start ->
    buffer.putInt(start + WORD_SIZE, ADRP_X8_REDIRECT_PAGE.toInt())
    buffer.putInt(start + 2 * WORD_SIZE, ADD_X8_REDIRECT_OFFSET.toInt())
}

private val returnAllProductPlan: (ByteBuffer, Int) -> Unit = { buffer, start ->
    buffer.putInt(start, MOV_W0_ALL_PRODUCT_PLAN)
    buffer.putInt(start + WORD_SIZE, RET.toInt())
}

private val patchSites = listOf(
    PatchSite(intArrayOf(0x90039108u.toInt(), 0xb90b0913u.toInt(), 0x14000005), clearDoodleFlagStore),
    PatchSite(intArrayOf(0xf00390e8u.toInt(), 0xb90b0913u.toInt(), 0xb9069533u.toInt()), clearDoodleFlagStore),
    PatchSite(intArrayOf(0x52800029, 0xb90b0909u.toInt(), 0xf00376c0u.toInt()), clearDoodleFlagStore),
    PatchSite(intArrayOf(0xb00380abu.toInt(), 0xb90b0968u.toInt(), 0x7100055f), clearDoodleFlagStore),
    PatchSite(intArrayOf(0xb00380a9u.toInt(), 0xb90b0935u.toInt(), 0x350001c8), clearDoodleFlagStore),
    PatchSite(
        intArrayOf(0x54000081, 0x90025528u.toInt(), 0x912c2108u.toInt(), 0x14000018),
        redirectDoodleFlagStore,
    ),
    PatchSite(
        intArrayOf(
            0xd102c3ffu.toInt(),
            0xa9067bfdu.toInt(),
            0xf9003bf9u.toInt(),
            0xa9085ff8u.toInt(),
            0xa90957f6u.toInt(),
            0xa90a4ff4u.toInt(),
            0x910183fdu.toInt(),
            0xd53bd054u.toInt(),
            0xf9401688u.toInt(),
            0xf81f83a8u.toInt(),
        ),
        returnAllProductPlan,
    ),
)

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks saving and exporting in doodle mode, including full-length timelapse export with an optional logo.",
) {
    compatibleWith(AppCompatibilities.CLIP_STUDIO_PAINT)
    availability(requireArm64)

    execute {
        rewriteNativeLibrary(patchSites)
    }
}
