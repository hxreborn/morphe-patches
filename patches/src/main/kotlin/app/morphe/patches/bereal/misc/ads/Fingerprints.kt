/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AdRequestIdToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("AdRequestId(id="),
)

internal object AdPlacementEnumFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("FOF", "DISCOVERY", "ALL", "FRIENDS"),
)

internal fun adSelectorFingerprint(adRequestType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(adRequestType),
    filters = listOf(opcode(Opcode.MONITOR_ENTER)),
)

internal fun adProviderSetupFingerprint(adsManagerType: String, placementType: String) = Fingerprint(
    definingClass = adsManagerType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", placementType, "L"),
)

internal object NativeAdLoaderFingerprint : Fingerprint(
    strings = listOf("AppLovin instance not initialized"),
)

internal fun nativeAdBatchLoadFingerprint(loaderType: String) = Fingerprint(
    definingClass = loaderType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/util/List;", "L"),
    filters = listOf(methodCall(name = "tryLock")),
)

internal object SpotlightAdLoadFingerprint : Fingerprint(
    name = "invokeSuspend",
    filters = listOf(
        methodCall(definingClass = "Lio/adn/sdk/publisher/AdnSdk;", name = "getNativeAdInstance"),
    ),
)

internal object AdsSetupFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("INIT_ADS ", "Initializing SDK "),
)

internal object AdnInitializeFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Lio/adn/sdk/publisher/AdnSdk;", name = "initialize"),
    ),
)

internal fun spotlightAdEntryFingerprint(loaderType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    filters = listOf(newInstance(loaderType)),
)

internal object ServerFeedAdsExperimentFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("ads-s2s-fof-android", "activate"),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        methodCall(
            name = "<init>",
            parameters = listOf("Z"),
            returnType = "V",
            opcode = Opcode.INVOKE_DIRECT,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object FeedAddMessageFingerprint : Fingerprint(
    name = "dynamicMethod",
    strings = listOf("posts_", "ad_"),
)

internal fun feedAddHasAdFingerprint(feedAddType: String) = Fingerprint(
    definingClass = feedAddType,
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(fieldAccess(definingClass = feedAddType, name = "bitField0_", opcode = Opcode.IGET)),
)

internal object FeedPostEntryMessageFingerprint : Fingerprint(
    name = "dynamicMethod",
    strings = listOf("id_", "isSponsored_"),
)

internal fun feedPostEntryLoopFingerprint(postEntryType: String) = Fingerprint(
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Iterable;", name = "iterator"),
        checkCast(postEntryType, location = MatchAfterWithin(6)),
    ),
)
