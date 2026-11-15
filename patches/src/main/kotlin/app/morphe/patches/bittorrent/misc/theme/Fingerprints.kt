/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val UI_MODE_NIGHT_MASK = 0x30L
private const val UI_MODE_NIGHT_YES = 0x20L

internal fun surfaceResourceFingerprint(resourceName: String) = Fingerprint(
    filters = listOf(fieldAccess(name = resourceName, type = "I", opcode = Opcode.SGET)),
)

internal object DarkModeFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Landroid/content/SharedPreferences;",
        ),
        methodCall(name = "getConfiguration", definingClass = "Landroid/content/res/Resources;"),
        fieldAccess(name = "uiMode", definingClass = "Landroid/content/res/Configuration;"),
        literal(UI_MODE_NIGHT_MASK),
        literal(UI_MODE_NIGHT_YES),
    ),
)

internal object SettingsResumeFingerprint : Fingerprint(
    definingClass = "Lcom/bittorrent/setting/activity/SettingActivity;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
)
