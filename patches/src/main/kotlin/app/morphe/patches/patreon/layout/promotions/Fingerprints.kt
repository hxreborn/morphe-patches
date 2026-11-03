/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal object FeedBannerCardFingerprint : Fingerprint(
    definingClass = "Lcom/patreon/android/data/api/network/requestobject/FeedItemSchemaKt;",
    name = "feedBannerCard",
    returnType = "Lcom/patreon/android/data/api/network/requestobject/FeedBannerSchema;",
)

internal object UpsellSheetStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(string("ShowFreeMembershipUpsellBottomSheet(campaignId=")),
)

internal fun upsellStateDispatcherFingerprint(stateInterface: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(stateInterface, "L", "L", "L", "Lkotlin/jvm/functions/Function1;"),
    custom = { _, classDef -> classDef.sourceFile == "FreeMembershipUpsellHandler.kt" },
)
