/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/app/AEApp;",
    name = "onCreate",
    parameters = emptyList(),
    returnType = "V",
)

internal object SettingsViewFingerprint : Fingerprint(
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    returnType = "Landroid/view/View;",
    filters = listOf(resourceLiteral(ResourceType.ID, "rl_version_settings")),
)

internal object SwitchConstructorFingerprint : Fingerprint(
    definingClass = "Landroidx/appcompat/widget/SwitchCompat;",
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
    returnType = "V",
)
