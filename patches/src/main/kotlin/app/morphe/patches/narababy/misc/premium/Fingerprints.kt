/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.premium

import app.morphe.patcher.Fingerprint

internal object ResponseBuilderBuildFingerprint : Fingerprint(
    definingClass = "Lokhttp3/Response\$Builder;",
    name = "build",
    returnType = "Lokhttp3/Response;",
    parameters = emptyList(),
)
