/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.channelusage

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patches.netanalyzer.shared.decryptedString
import com.android.tools.smali.dexlib2.Opcode

internal const val USAGE_PAGE = 2
private const val CHANNEL_GRAPH = "Channel Graph"
private const val OBJECT_TYPE_PREFIX = "L"
private const val OBJECT_TYPE = "Ljava/lang/Object;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val INT_TYPE = "I"

private val dropdownMenuItemCall = methodCall(
    opcode = Opcode.INVOKE_STATIC,
    parameters = listOf(STRING_TYPE, OBJECT_TYPE_PREFIX, OBJECT_TYPE_PREFIX, OBJECT_TYPE_PREFIX, INT_TYPE),
    returnType = "V",
)

internal object ChannelUsageMenuItemFingerprint : Fingerprint(
    filters = listOf(decryptedString(CHANNEL_GRAPH), dropdownMenuItemCall, dropdownMenuItemCall),
)

internal fun pageSelectionFingerprint(onClickType: String) = Fingerprint(
    definingClass = onClickType,
    returnType = OBJECT_TYPE,
    parameters = listOf(),
    filters = listOf(
        literal(USAGE_PAGE),
        methodCall(parameters = listOf(INT_TYPE), returnType = "V", location = MatchAfterImmediately()),
        methodCall(parameters = listOf(CONTEXT_TYPE), returnType = "V", location = MatchAfterImmediately()),
    ),
)
