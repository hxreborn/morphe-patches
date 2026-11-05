/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.settings

import app.morphe.patcher.Fingerprint

internal object SettingsViewFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("comparison_setting_page_entry"),
)

internal object ApplicationCreateFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/shell/ShellApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)
