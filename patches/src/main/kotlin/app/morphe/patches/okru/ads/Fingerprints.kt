/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.okru.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.Opcode

private const val INSTREAM_AD_CONTROLLER_TYPE = "Lone/video/ad/AdController;"
private const val ZEN_ADS_INTERACTOR_CLASS = "Lru/zen/ad/domain/AdsInteractorImpl;"
private const val LIST_TYPE = "Ljava/util/List;"
private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val CONTINUATION_TYPE = "Lkotlin/coroutines/Continuation;"

internal object BindReplaceableBannerFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("ADV_SHOW_FEATURE"),
)

internal object BindBannerLinkFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("RED_LINK_SHOW_FEATURE"),
)

internal object ObtainInstreamAdControllerFingerprint : Fingerprint(
    returnType = INSTREAM_AD_CONTROLLER_TYPE,
    filters = listOf(newInstance(INSTREAM_AD_CONTROLLER_TYPE)),
)

internal object MyTargetIsLoadCalledFingerprint : Fingerprint(
    definingClass = "Lcom/my/target/common/BaseAd;",
    name = "isLoadCalled",
    returnType = "Z",
    parameters = emptyList(),
)

internal object MyTargetViewLoadFingerprint : Fingerprint(
    definingClass = "Lcom/my/target/ads/MyTargetView;",
    name = "load",
    returnType = "V",
    parameters = emptyList(),
)

internal object LoadDiscussionBannerFingerprint : Fingerprint(
    definingClass = "Lru/ok/android/discussions/presentation/comments/DiscussionViewModel;",
    name = "loadBannerIfNeed",
    returnType = "V",
    parameters = emptyList(),
)

internal object ZenLoadAdRequestFingerprint : Fingerprint(
    definingClass = ZEN_ADS_INTERACTOR_CLASS,
    name = "loadAd",
    parameters = listOf("Lru/zen/ad/domain/AdsInteractor\$AdRequest;", "Z", CONTEXT_TYPE, CONTINUATION_TYPE),
)

internal object ZenLoadAdRequestListFingerprint : Fingerprint(
    definingClass = ZEN_ADS_INTERACTOR_CLASS,
    name = "loadAd",
    parameters = listOf(
        LIST_TYPE,
        "Z",
        "Lkotlin/jvm/functions/Function1;",
        CONTEXT_TYPE,
        CONTINUATION_TYPE,
    ),
)

internal object GamesAdSourcesFingerprint : Fingerprint(
    returnType = LIST_TYPE,
    parameters = emptyList(),
    strings = listOf("games.allow.ads"),
)

internal object VideoCatalogLoadBannersFingerprint : Fingerprint(
    definingClass = "Lru/ok/android/ui/video/fragments/movies/loaders/GetCatalogRequestExecutor;",
    filters = listOf(fieldAccess(name = "isLoadBanners", opcode = Opcode.IGET_BOOLEAN)),
)

internal fun adFlagFingerprint(key: String) = Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf(key),
)
