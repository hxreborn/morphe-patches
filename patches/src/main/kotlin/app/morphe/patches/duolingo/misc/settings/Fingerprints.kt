/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val GROUPED_LIST_ITEM_POSITION_CLASS =
    "Lcom/duolingo/core/design/compose/components/GroupedListItemPosition;"
internal const val STRING_TYPE = "Ljava/lang/String;"
private const val FEEDBACK_ROW_CALL_WINDOW = 4

internal object SupportSectionFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("helpCenterRowItem"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC_RANGE,
            returnType = "V",
            parameters = listOf(STRING_TYPE, "L", "Z", "L", "I", "I"),
        ),
        string("feedbackRowItem"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC_RANGE,
            returnType = "V",
            parameters = listOf(
                STRING_TYPE,
                GROUPED_LIST_ITEM_POSITION_CLASS,
                "L",
                STRING_TYPE,
                STRING_TYPE,
                "Z",
                "Ljava/lang/Integer;",
                STRING_TYPE,
                "F",
                "Lkotlin/jvm/functions/Function0;",
                "L",
                "L",
                "I",
                "I",
            ),
            location = MatchAfterWithin(FEEDBACK_ROW_CALL_WINDOW),
        ),
    ),
)
