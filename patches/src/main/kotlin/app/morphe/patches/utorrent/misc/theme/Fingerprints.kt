/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal fun surfaceResourceFingerprint(resourceName: String) = Fingerprint(
    filters = listOf(fieldAccess(name = resourceName, type = "I", opcode = Opcode.SGET)),
)

internal fun darkModeFingerprint(themeClass: String) = Fingerprint(
    definingClass = themeClass,
    isStatic = true,
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(methodCall(name = "getConfiguration", definingClass = "Landroid/content/res/Resources;")),
)

internal object SettingsThemeFingerprint : Fingerprint(
    definingClass = "Lcom/bittorrent/app/settings/SettingActivity;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(fieldAccess(name = "icon_theme"), fieldAccess(name = "icon_bandwidth")),
)

internal object LowPowerBannerFingerprint : Fingerprint(
    definingClass = "Lcom/bittorrent/app/view/LowPowerNotificationView;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(name = "valueOf", definingClass = "Ljava/lang/Integer;"),
        methodCall(name = "setVisibility", parameters = listOf("I"), returnType = "V"),
    ),
)
