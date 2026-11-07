/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.home

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub

private const val SCHEMA_PACKAGE = "Lcom/patreon/android/data/api/network/requestobject"
private const val BASE_FEED_ITEM = "$SCHEMA_PACKAGE/BaseFeedItemSchema;"
private const val LAUNCHER_FEED_ITEM = "$SCHEMA_PACKAGE/LauncherFeedItemSchema;"
private const val FEEDS_V2_FEED_ITEM = "$SCHEMA_PACKAGE/FeedsV2LauncherFeedItemSchema;"
private const val BASE_FEED_SECTION = "$SCHEMA_PACKAGE/BaseFeedSectionSchema;"
private const val FEEDS_V2_FEED_SECTION = "$SCHEMA_PACKAGE/FeedsV2LauncherFeedSectionSchema;"
private const val LAUNCHER_CARD_POST = "$SCHEMA_PACKAGE/LauncherCardPostSchema;"
private const val FEEDS_V2_CARD_POST = "$SCHEMA_PACKAGE/FeedsV2LauncherCardPostSchema;"
private const val NETWORK_SCHEMA = "Lcom/patreon/android/network/intf/schema/NetworkSchema;"

private class FeedGetter(val definingClass: String, val name: String)

private fun BytecodePatchContext.replaceGetterStub(extensionClass: String, stub: String, vararg getters: FeedGetter) {
    val branches = getters.mapIndexed { index, getter ->
        val method = Fingerprint(definingClass = getter.definingClass, name = getter.name, parameters = emptyList())
            .matchSingle().originalMethod
        """
            instance-of v0, p0, ${getter.definingClass}
            if-eqz v0, :other$index
            check-cast p0, ${getter.definingClass}
            invoke-virtual { p0 }, ${descriptor(method)}
            move-result-object v0
            return-object v0
            :other$index
        """.trimIndent()
    }
    mutableClassDefBy(extensionClass).replaceStub(
        stub,
        2,
        branches.joinToString("\n") + "\nconst/4 v0, 0x0\nreturn-object v0",
    )
}

internal fun BytecodePatchContext.replaceHomeFeedStubs(extensionClass: String) {
    replaceGetterStub(
        extensionClass,
        "postCardOf",
        FeedGetter(BASE_FEED_ITEM, "getPostCard"),
        FeedGetter(FEEDS_V2_FEED_ITEM, "getPostCard"),
    )
    replaceGetterStub(
        extensionClass,
        "postOf",
        FeedGetter(LAUNCHER_CARD_POST, "getPost"),
        FeedGetter(FEEDS_V2_CARD_POST, "getPost"),
    )
    replaceGetterStub(extensionClass, "stringIdOf", FeedGetter(NETWORK_SCHEMA, "getStringId"))
    replaceGetterStub(
        extensionClass,
        "recsplanationDataOf",
        FeedGetter(BASE_FEED_ITEM, "getRecsplanationData"),
        FeedGetter(FEEDS_V2_FEED_ITEM, "getRecsplanationData"),
    )
    replaceGetterStub(
        extensionClass,
        "recommendedCreatorCardOf",
        FeedGetter(LAUNCHER_FEED_ITEM, "getRecommendedCreatorCard"),
    )
    replaceGetterStub(
        extensionClass,
        "sectionTypeIdOf",
        FeedGetter(BASE_FEED_SECTION, "getRawSectionTypeId"),
        FeedGetter(FEEDS_V2_FEED_SECTION, "getRawSectionTypeId"),
    )
}

internal fun BytecodePatchContext.replaceQuickPostCardStub(extensionClass: String) =
    replaceGetterStub(extensionClass, "quickPostCardOf", FeedGetter(LAUNCHER_FEED_ITEM, "getQuickPostCard"))
