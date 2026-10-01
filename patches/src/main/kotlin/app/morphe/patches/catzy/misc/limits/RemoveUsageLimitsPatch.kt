/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.limits

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.catzy.catzyPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val RECYCLE_SHOP_FRAGMENT_CLASS = "Lcom/nieruo/healthapp/ui/fragment/ShopRecycleFragment;"
private const val DAY_OF_WEEK_CLASS = "Lj\$/time/DayOfWeek;"

private val DAILY_USE_COUNTERS = listOf(
    "blind_box_furniture_count",
    "blind_box_outfit_count",
    "book_answer_count",
    "day_coin_count",
    "feeding_count",
    "furniture_refresh_count",
    "outfit_refresh_count",
    "petting_count",
)

@Suppress("unused")
val removeUsageLimitsPatch = catzyPatch(
    name = "Remove usage limits",
    description = "Removes daily caps on store refreshes, blind boxes, feeding, petting and the Book of Answers. " +
        "Opens the Item Recycling Center every day and shortens pet exploration to three minutes.",
    selection = PatchAvailability.DISABLED,
) {
    DAILY_USE_COUNTERS.forEach { property ->
        dailyUseCounterFingerprint(property).matchSingle().method.returnEarly(0)
    }

    val openingDayCheck = mutableClassDefBy(RECYCLE_SHOP_FRAGMENT_CLASS).methods.singleOrNull { method ->
        method.implementation != null &&
            method.instructions.any {
                val reference = it.getReference<MethodReference>()
                reference?.definingClass == DAY_OF_WEEK_CLASS && reference.name == "getValue"
            }
    } ?: throw PatchException("No opening day check in $RECYCLE_SHOP_FRAGMENT_CLASS")

    openingDayCheck.removeInstruction(
        openingDayCheck.indexOfFirstInstructionOrThrow { opcode == Opcode.IF_NE },
    )

    TravelDurationFingerprint.matchSingle().let { match ->
        match.method.removeInstruction(
            match.method.indexOfFirstInstructionReversedOrThrow(
                match.instructionMatches.first().index,
                Opcode.IF_EQZ,
            ),
        )
    }
}
