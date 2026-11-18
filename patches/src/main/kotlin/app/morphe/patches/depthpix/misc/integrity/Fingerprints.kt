/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.integrity

import app.morphe.patcher.Fingerprint

internal object ClientIntegrityAttestationFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("/client-integrity/challenge"),
)
