/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val LIFETIME_PLAN_CLASS = "Lcom/oneweather/premium/domain/model/UserPlan\$LifetimePlan;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium and removes ads.",
) {
    compatibleWith(AppCompatibilities.ONE_WEATHER)

    execute {
        PremiumUserFingerprint.matchSingle().method.returnEarly(true)

        PremiumUserFlowFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static { v0 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static { v0 }, Lkotlinx/coroutines/flow/FlowKt;->flowOf(Ljava/lang/Object;)Lkotlinx/coroutines/flow/Flow;
                move-result-object v0
                return-object v0
            """,
        )

        SyncPremiumDataFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static { v0 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static { v0 }, Lkotlin/Result;->constructor-impl(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                return-object v0
            """,
        )

        val lifetimePlan = classDefBy(LIFETIME_PLAN_CLASS).staticFields
            .singleOrNull { it.type == LIFETIME_PLAN_CLASS }
            ?: throw PatchException("$LIFETIME_PLAN_CLASS has no singleton field")

        UserPlanFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, $LIFETIME_PLAN_CLASS->${lifetimePlan.name}:$LIFETIME_PLAN_CLASS
                return-object v0
            """,
        )
    }
}
