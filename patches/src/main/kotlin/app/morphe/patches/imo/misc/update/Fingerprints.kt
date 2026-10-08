/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.update

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val UPDATE_CONFIG_CLASS = "Lcom/imo/android/imoim/update/data/ImoUpdateConfig;"

internal object UpdateDialogFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", UPDATE_CONFIG_CLASS, "Z", "Z"),
    strings = listOf("showAllowDismissUpdateDialog: false because of time interval"),
)

internal object UpdateScreenLauncherFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/imoim/update/UpdateActivity3\$",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", UPDATE_CONFIG_CLASS),
    filters = listOf(methodCall(definingClass = "Landroid/content/Context;", name = "startActivity")),
)

internal object VersionCheckResultFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("version too old can't use it anymore :("),
)

internal object InAppUpdateCheckFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Z"),
    strings = listOf("check flexible update? "),
)
