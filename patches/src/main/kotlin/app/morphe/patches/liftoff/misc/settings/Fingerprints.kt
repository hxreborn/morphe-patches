/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.liftoff.misc.settings

import app.morphe.patcher.Fingerprint

internal fun applicationOnCreateFingerprint(applicationType: String) = Fingerprint(
    definingClass = applicationType,
    name = "onCreate",
    isStatic = false,
    parameters = emptyList(),
    returnType = "V",
)
