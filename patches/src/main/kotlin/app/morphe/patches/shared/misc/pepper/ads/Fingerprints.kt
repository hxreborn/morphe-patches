/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AD_LOADER = "Lcom/google/android/gms/ads/AdLoader;"
private const val AD_VIEW_CONTAINER = "Lcom/pepper/presentation/ads/AdViewContainer;"
private const val AATKIT = "Lcom/intentsoftware/addapptr/AATKit;"
private const val JSON_OBJECT = "Lorg/json/JSONObject;"
private const val POB_BANNER_VIEW = "Lcom/pubmatic/sdk/openwrap/banner/POBBannerView;"
private const val STRING = "Ljava/lang/String;"
private const val SPONSORED_TO_AD_ITEM_WINDOW = 900
private const val AD_ITEM_TO_LIST_ADD_WINDOW = 500
private const val AD_ITEM_CONSTRUCTOR_WINDOW = 80
private const val DETAIL_AD_CALL_WINDOW = 200

internal object ExplorationAdItemFingerprint : Fingerprint(
    strings = listOf("ExplorationAdItem(item="),
)

internal object ExplorationSponsoredItemFingerprint : Fingerprint(
    strings = listOf("ExplorationSponsoredItem(item="),
)

internal object SmallAdItemFingerprint : Fingerprint(
    strings = listOf("Small(id=", ", shouldRequestAdLoad="),
)

internal object MediumAdItemFingerprint : Fingerprint(
    strings = listOf("Medium(id=", ", shouldRequestAdLoad="),
)

internal object LargeAdItemFingerprint : Fingerprint(
    strings = listOf("Large(id=", ", shouldRequestAdLoad="),
)

internal object InterscrollerAdItemFingerprint : Fingerprint(
    strings = listOf("Interscroller(id=", ", shouldRequestAdLoad="),
)

internal object GoogleMobileAdsInitFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/MobileAds;",
            name = "initialize",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)

internal object SponsoredThreadAdLoadFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", STRING, STRING, STRING, JSON_OBJECT, "I", "L"),
    strings = listOf("callGoogleAdManager() loadAd() for "),
    filters = listOf(
        methodCall(
            definingClass = AD_LOADER,
            name = "loadAd",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

internal object OpenWrapBannerLoadFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf(AD_VIEW_CONTAINER, "L", "L"),
    filters = listOf(
        methodCall(
            definingClass = POB_BANNER_VIEW,
            name = "loadAd",
            parameters = emptyList(),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

internal object AatKitBannerLoadFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf(AD_VIEW_CONTAINER, "L", "L"),
    filters = listOf(
        methodCall(
            definingClass = AATKIT,
            name = "setUserTargeting",
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)

internal object AatKitBannerPrefetchFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            definingClass = AATKIT,
            name = "setUserTargeting",
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)

internal fun aatKitInitFingerprint(adInitializerType: String) = Fingerprint(
    definingClass = adInitializerType,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/Context;",
            name = "getApplicationContext",
            parameters = emptyList(),
            returnType = "Landroid/content/Context;",
        ),
    ),
)

internal fun adMediationInitFingerprint(adInitializerType: String) = Fingerprint(
    definingClass = adInitializerType,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Z", "L"),
)

internal fun explorationItemMapperFingerprint(sponsoredItemType: String, adItemType: String) = Fingerprint(
    returnType = "L",
    filters = listOf(
        instanceOf(sponsoredItemType),
        instanceOf(adItemType, location = MatchAfterWithin(SPONSORED_TO_AD_ITEM_WINDOW)),
        methodCall(
            definingClass = "Ljava/util/ArrayList;",
            name = "add",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterWithin(AD_ITEM_TO_LIST_ADD_WINDOW),
        ),
    ),
)

internal fun adItemFactoryFingerprint(adItemTypes: List<String>) = Fingerprint(
    returnType = "L",
    filters = listOf(
        methodCall(
            definingClass = adItemTypes.first(),
            name = "<init>",
            opcode = Opcode.INVOKE_DIRECT_RANGE,
        ),
    ) + adItemTypes.drop(1).map { adItemType ->
        methodCall(
            definingClass = adItemType,
            name = "<init>",
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            location = MatchAfterWithin(AD_ITEM_CONSTRUCTOR_WINDOW),
        )
    },
)

internal fun detailAdSlotsFingerprint(adItemFactoryMethod: MethodReference) = Fingerprint(
    filters = listOf(
        methodCall(reference = adItemFactoryMethod),
        methodCall(reference = adItemFactoryMethod, location = MatchAfterWithin(DETAIL_AD_CALL_WINDOW)),
    ),
)
