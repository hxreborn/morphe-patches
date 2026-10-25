/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.systembars

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string

internal object AppThemeFingerprint : Fingerprint(
    filters = listOf(
        string("work.space.superapp.presentation.base.BaseActivity.CleanArchitectureTheme (", StringComparisonType.STARTS_WITH),
    ),
)

internal object BaseActivityOnCreateFingerprint : Fingerprint(
    classFingerprint = AppThemeFingerprint,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
