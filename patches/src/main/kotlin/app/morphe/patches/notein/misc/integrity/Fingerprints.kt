/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notein.misc.integrity

import app.morphe.patcher.Fingerprint

internal object ShowRemediationDialogFingerprint : Fingerprint(
    definingClass = "Lcom/orion/notein/MainActivity;",
    name = "showRemediationDialogIfNeeded",
    returnType = "V",
    parameters = emptyList(),
)
