/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val FEED_ITEM_TYPE_CLASS = "Lcom/nextdoor/feedmodel/NewsFeedRecyclerViewItemType;"

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
