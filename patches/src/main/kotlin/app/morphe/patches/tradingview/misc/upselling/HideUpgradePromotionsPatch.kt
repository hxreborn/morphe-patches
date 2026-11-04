/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities

private const val BANNER_MODEL_PACKAGE = "Lcom/tradingview/tradingviewapp/feature/banner/api/model"
private const val PAYMENT_ERROR_BANNER = "$BANNER_MODEL_PACKAGE/ProfileBanner\$PaymentErrorBanner;"
private const val GO_PRO_WATCHLIST_BANNER = "$BANNER_MODEL_PACKAGE/GoProWatchlistBanner;"
private const val BLACK_FRIDAY_TEASER_BANNER = "$BANNER_MODEL_PACKAGE/WatchlistBanner\$BlackFridayTeaserHeader;"
private const val REFER_FRIEND_ITEM = "Lcom/tradingview/tradingviewapp/feature/menu/impl/state/MenuItem\$ReferFriend;"

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Upgrade button, plan banners, subscription card and paywalls. " +
        "Extra indicators, watchlists, flag colors, alerts, second and tick intervals, " +
        "and intraday replay are not unlocked.",
) {
    compatibleWith(AppCompatibilities.TRADINGVIEW)

    execute {
        PaywallEventPushFingerprint.matchSingle().method.returnUnit()
        GoProActionDispatchFingerprint.matchSingle().method.returnEarly()
        UpgradeButtonParamsFingerprint.matchSingle().method.returnStateFlowOfNull()
        MenuBannerContentFingerprint.matchSingle().method
            .returnNullUnlessFirstParameterIsInstanceOf(PAYMENT_ERROR_BANNER)
        MenuProfileItemsFingerprint.matchSingle().let { match ->
            match.method.returnOnlyReferFriend(match.classDef.type)
        }
        WatchlistBannerUpdateFingerprint.matchSingle().method.nullFirstParameterIfInstanceOf(
            GO_PRO_WATCHLIST_BANNER,
            BLACK_FRIDAY_TEASER_BANNER,
        )
    }
}

private fun MutableMethod.returnUnit() = addInstructions(
    0,
    """
        sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
        return-object v0
    """,
)

private fun MutableMethod.returnStateFlowOfNull() = addInstructions(
    0,
    """
        const/4 v0, 0x0
        invoke-static { v0 }, Lkotlinx/coroutines/flow/StateFlowKt;->MutableStateFlow(Ljava/lang/Object;)Lkotlinx/coroutines/flow/MutableStateFlow;
        move-result-object v0
        return-object v0
    """,
)

private fun MutableMethod.returnNullUnlessFirstParameterIsInstanceOf(type: String) = addInstructionsWithLabels(
    0,
    """
        instance-of v0, p1, $type
        if-nez v0, :keep
        const/4 v0, 0x0
        return-object v0
    """,
    ExternalLabel("keep", getInstruction(0)),
)

private fun MutableMethod.returnOnlyReferFriend(stateType: String) = addInstructions(
    0,
    """
        iget-object v0, p0, $stateType->referFriend:$REFER_FRIEND_ITEM
        invoke-static { v0 }, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;
        move-result-object v0
        return-object v0
    """,
)

private fun MutableMethod.nullFirstParameterIfInstanceOf(vararg types: String) {
    val clearWhenMatched = types.joinToString("\n") { type -> "instance-of v0, p1, $type\nif-nez v0, :clear" }
    addInstructionsWithLabels(
        0,
        """
            $clearWhenMatched
            goto :keep
            :clear
            const/4 p1, 0x0
        """,
        ExternalLabel("keep", getInstruction(0)),
    )
}
