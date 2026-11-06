/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.gaps

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object ChatFooterHeightFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.DIMEN, "messages_list_empty_view_under_fab_height"),
        methodCall(definingClass = "Landroid/content/res/Resources;", name = "getDimensionPixelSize"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        string("search_state_id_extra"),
    ),
)

internal object MoreListBindingFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "more_list"),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object ChatListBindingFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "messages"),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)
