/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.shorts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

internal const val TODAY_CARDS_ORDER_CLASS =
    "Lcom/oneweather/remotelibrary/sources/firebase/models/TodayCardsOderMap;"

@Suppress("unused")
val hideShortsPatch = bytecodePatch(
    name = "Hide Shorts",
    description = "Hides the 1Weather Shorts card from the Today screen.",
) {
    compatibleWith(AppCompatibilities.ONE_WEATHER)

    execute {
        val todayCards = classDefBy(TODAY_CARDS_ORDER_CLASS).instanceFields
            .singleOrNull { it.type == "Ljava/util/Map;" }
            ?: throw PatchException("$TODAY_CARDS_ORDER_CLASS has no card map field")

        TodayCardsFingerprint.matchSingle().method.addInstructions(
            0,
            """
                iget-object v0, p0, $TODAY_CARDS_ORDER_CLASS->${todayCards.name}:Ljava/util/Map;
                new-instance p0, Ljava/util/LinkedHashMap;
                invoke-direct { p0, v0 }, Ljava/util/LinkedHashMap;-><init>(Ljava/util/Map;)V
                const-string v0, "SHORTS"
                invoke-virtual { p0, v0 }, Ljava/util/LinkedHashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;
                return-object p0
            """,
        )
    }
}
