/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.toolshistory

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.netanalyzer.shared.decryptedString
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val NAVIGATION_FAILURE = "Cannot navigate to "
private const val HISTORY_PREFERENCES = "ToolsHistory"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val INT_TYPE = "I"
private const val OBJECT_TYPE_PREFIX = "L"
private const val PREFERENCES_EDITOR = "Landroid/content/SharedPreferences\$Editor;"
private const val URI = "Landroid/net/Uri;"

internal object NavigateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(OBJECT_TYPE_PREFIX, STRING_TYPE),
    strings = listOf(NAVIGATION_FAILURE),
)

internal fun historyRecorderFingerprint(itemType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(itemType),
    filters = listOf(
        decryptedString(HISTORY_PREFERENCES),
        methodCall(definingClass = PREFERENCES_EDITOR, name = "putString"),
    ),
)

internal fun queryRouteNavigationFingerprint(itemType: String) = Fingerprint(
    filters = listOf(
        methodCall(definingClass = itemType, name = "<init>", parameters = listOf(STRING_TYPE, INT_TYPE)),
        opcode(Opcode.CONST_CLASS),
        methodCall(definingClass = URI, name = "encode"),
    ),
)
