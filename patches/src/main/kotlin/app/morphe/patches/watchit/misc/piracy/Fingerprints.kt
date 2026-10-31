/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.piracy

import app.morphe.patcher.Fingerprint

internal object PiracyCheckerStartFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    strings = listOf("\$this\$verifySigningCertificate", "\$this\$verifyInstallerId"),
)
