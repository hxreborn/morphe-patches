/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.tradingview.misc.upselling

import app.morphe.patcher.Fingerprint

internal object PaywallEventPushFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/paywalls/impl/service/PaywallEventsDelegate;",
    name = "push",
    returnType = "Ljava/lang/Object;",
)

internal object GoProActionDispatchFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/gopro/impl/core/interactor/GoProTypeInteractorImpl;",
    name = "dispatchAction",
    returnType = "V",
    parameters = listOf("Lcom/tradingview/tradingviewapp/gopro/api/model/BaseGoProAction;"),
)

internal object UpgradeButtonParamsFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/gopro/impl/upgradebutton/UpgradeParamsInteractorImpl;",
    name = "getParamsFlow",
    returnType = "Lkotlinx/coroutines/flow/Flow;",
    parameters = listOf("Lcom/tradingview/tradingviewapp/gopro/api/view/upgradebutton/HostScreen;"),
)

internal object MenuBannerContentFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/feature/menu/impl/state/ProfileBannerContentMapper;",
    name = "mapToBannerContentInfo",
)

internal object MenuProfileItemsFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/feature/menu/impl/state/MenuSectionState;",
    name = "profileMenuItems",
    returnType = "Ljava/util/List;",
)

internal object WatchlistBannerUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/tradingview/tradingviewapp/watchlist/impl/module/symbols/service/WatchlistBannerServiceImpl;",
    name = "updateBanner",
    returnType = "V",
)
