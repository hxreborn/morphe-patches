/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

internal object MainSettingsItemsFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "mail_settings_folders_labels")),
)
