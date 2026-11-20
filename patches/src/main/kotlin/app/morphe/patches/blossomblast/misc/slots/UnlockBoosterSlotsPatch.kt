/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.misc.slots

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patches.blossomblast.shared.MODIFIERS
import app.morphe.patches.blossomblast.shared.blossomBlastModifiersPatch
import app.morphe.patches.blossomblast.shared.replaceArrayStub
import app.morphe.patches.blossomblast.shared.replaceStringStub
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.replaceStub

private const val BOOSTER_SLOT_TYPE_NAME = "NSt6__ndk110__function6__funcINS_6__bindIM20CPreLevelPopupScreen" +
    "Fv12EBoosterTypeEJPS3_RS4_EEENS_9allocatorIS9_EEFvvEEE"
private const val BOOSTER_ENTRY_SIZE = 0x1c
private const val BOOSTER_TYPE_OFFSET = 0xc
private const val UNLOCK_LEVEL_OFFSET = 0x14
private val STOCK_BOOSTER_TYPES = listOf(0, 1, 2, 12, 3, 5, 12)
private val STOCK_UNLOCK_LEVELS = listOf(5, 36, 52, 0, 0, 0, 0)
private const val INT_WIDTH = 4

@Suppress("unused")
val unlockBoosterSlotsPatch = bytecodePatch(
    name = "Unlock booster slots",
    description = "Opens all booster slots from level 2.",
) {
    compatibleWith(AppCompatibilities.BLOSSOM_BLAST_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(blossomBlastModifiersPatch)

    execute {
        mutableClassDefBy(MODIFIERS).apply {
            replaceStub("slotsEnabled", 1, """
                const/4 v0, 0x1
                return v0
            """)
            replaceStringStub("boosterSlotTypeName", BOOSTER_SLOT_TYPE_NAME)
            replaceIntArrayStub("boosterSlotLayout", listOf(BOOSTER_ENTRY_SIZE, BOOSTER_TYPE_OFFSET, UNLOCK_LEVEL_OFFSET))
            replaceIntArrayStub("boosterSlotTypes", STOCK_BOOSTER_TYPES)
            replaceIntArrayStub("boosterSlotLevels", STOCK_UNLOCK_LEVELS)
        }
    }
}

private fun MutableClass.replaceIntArrayStub(name: String, values: List<Int>) =
    replaceArrayStub(name, "I", INT_WIDTH, values.map { it.toString() })
