/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.shared

import app.morphe.patcher.Fingerprint

private const val REMOTE_SETTINGS_CLASS = "Lcom/imo/android/imoim/setting/IMOSettingsDelegate;"

internal abstract class RemoteSettingFingerprint(getterName: String) :
    Fingerprint(definingClass = REMOTE_SETTINGS_CLASS, name = getterName)
