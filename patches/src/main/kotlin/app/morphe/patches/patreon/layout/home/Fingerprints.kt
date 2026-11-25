/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.home

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal fun launcherSectionItemsFingerprint(sectionSchema: String) = Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/$sectionSchema;",
    name = "getItems",
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_OBJECT)),
)

internal object RecommendedPostsPageItemsFingerprint : Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/RecommendedPostsFeedItemQueryNetworkResponse;",
    name = "getValue",
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_OBJECT)),
)

internal object RecommendedPostsQueryPostIdFingerprint : Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/queries/RecommendedPostsFeedQueryParameters;",
    name = "getPostId",
    returnType = "Lcom/patreon/android/database/model/ids/PostId;",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_OBJECT)),
)

internal object FeedSectionItemStatesFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(returnType = "Lcom/patreon/android/data/api/network/requestobject/FeedSectionSchema;", parameters = listOf()),
        methodCall(returnType = "Lcom/patreon/android/data/api/network/requestobject/FeedItemSchema;"),
        checkCast("Lcom/patreon/android/data/model/DataResult;"),
        methodCall(definingClass = "Lcom/patreon/android/data/model/DataResultKt;", name = "isNullFailure"),
    ),
)

internal object QuickPostCardStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("FeedQuickPostCardState(feedPostState="),
)

internal object LauncherFeedPostStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("LauncherFeedPostState(feedPostState="),
)

internal object FeedPostStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("FeedPostState(postId=", ", postOwner="),
)

internal object CampaignPostOwnerToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("Campaign(userId=", ", campaignId=", ", campaignVO="),
)

internal object LauncherFeedResponseConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/LauncherFeedQueryNetworkResponse;",
    name = "<init>",
    parameters = listOf("Ljava/util/List;", "Ljava/util/Map;", "[B", "Ljava/lang/String;", "Ljava/lang/Integer;"),
)
