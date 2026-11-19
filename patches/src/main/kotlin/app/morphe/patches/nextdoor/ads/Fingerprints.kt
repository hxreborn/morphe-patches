/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val FEED_ITEM_TYPE_CLASS = "Lcom/nextdoor/feedmodel/NewsFeedRecyclerViewItemType;"
internal const val ROLLUP_CARD_TYPE_CLASS = "Lcom/nextdoor/feedmodel/FeedRollupCardType;"
private const val AD_SURFACE_CLASS = "Lcom/nextdoor/ads/model/AdSurface;"
private const val AD_WRAPPER_TAIL_WINDOW = 2

internal const val SEARCH_RESULT_ITEM_TYPE_CLASS = "Lcom/nextdoor/apollo/type/SearchResultItemType;"
internal val CLASSIFIED_AD_ITEM_TYPES = listOf(
    "CLASSIFIEDS_GAM_ITEM",
    "CLASSIFIEDS_NAMPLUS_ITEM",
    "CLASSIFIEDS_NAS_ITEM",
)

internal val adPlaceholderTypeBranch = opcode(Opcode.IF_NE, location = MatchAfterImmediately())
internal val adTypeBranch = opcode(Opcode.IF_NE, location = MatchAfterImmediately())
internal val missingAdBranch = opcode(Opcode.IF_EQZ, location = MatchAfterImmediately())

private fun feedItemTypeCheck(itemType: String, branch: InstructionFilter) = listOf(
    fieldAccess(definingClass = FEED_ITEM_TYPE_CLASS, name = itemType, opcode = Opcode.SGET_OBJECT),
    methodCall(definingClass = FEED_ITEM_TYPE_CLASS, name = "getValue", location = MatchAfterImmediately()),
    opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    branch,
)

internal object AdsDataItemConverterFingerprint : Fingerprint(
    strings = listOf("Unexpected ad type in AdsDataItemConverter"),
    filters = feedItemTypeCheck("GAM_IS_LOADING", adPlaceholderTypeBranch) +
        feedItemTypeCheck("GAM_AD", adTypeBranch) + missingAdBranch,
)

internal val gridAdWrapperConstructor = methodCall(
    name = "<init>",
    parameters = listOf("Ljava/lang/String;", "L", "L"),
    opcode = Opcode.INVOKE_DIRECT,
)
internal val gridAdWrapperTail = opcode(Opcode.MOVE_OBJECT_FROM16, location = MatchAfterWithin(AD_WRAPPER_TAIL_WINDOW))
internal val gridTileConstructor = methodCall(name = "<init>", opcode = Opcode.INVOKE_DIRECT_RANGE)
internal val gridTileMove = opcode(Opcode.MOVE_OBJECT_FROM16, location = MatchAfterImmediately())
internal val gridTileNullCheck = opcode(Opcode.IF_EQZ, location = MatchAfterImmediately())
internal val gridTileAdd = methodCall(
    definingClass = "Ljava/util/ArrayList;",
    name = "add",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Z",
    opcode = Opcode.INVOKE_VIRTUAL,
    location = MatchAfterImmediately(),
)

internal object ClassifiedGridRollupFingerprint : Fingerprint(
    strings = listOf("fsf_feed_item_classified_image_ad"),
    filters = listOf(
        fieldAccess(definingClass = ROLLUP_CARD_TYPE_CLASS, name = "GRID_IMAGE_CARD", opcode = Opcode.SGET_OBJECT),
        gridAdWrapperConstructor,
        gridAdWrapperTail,
        gridTileConstructor,
        gridTileMove,
        gridTileNullCheck,
        gridTileAdd,
    ),
)

internal val searchItemTypeRead = fieldAccess(type = SEARCH_RESULT_ITEM_TYPE_CLASS, opcode = Opcode.IGET_OBJECT)
internal val searchItemTypeSwitchMap = fieldAccess(
    type = "[I",
    opcode = Opcode.SGET_OBJECT,
    location = MatchAfterImmediately(),
)
internal val searchItemTypeOrdinal = methodCall(
    definingClass = "Ljava/lang/Enum;",
    name = "ordinal",
    opcode = Opcode.INVOKE_VIRTUAL,
    location = MatchAfterImmediately(),
)
internal val searchItemTypeOrdinalResult = opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
internal val searchItemTypeSwitchIndex = opcode(Opcode.AGET, location = MatchAfterImmediately())
internal val searchItemTypeSwitch = opcode(Opcode.PACKED_SWITCH, location = MatchAfterImmediately())

internal object SearchResultItemConverterFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L", AD_SURFACE_CLASS),
    filters = listOf(
        searchItemTypeRead,
        searchItemTypeSwitchMap,
        searchItemTypeOrdinal,
        searchItemTypeOrdinalResult,
        searchItemTypeSwitchIndex,
        searchItemTypeSwitch,
    ),
)
