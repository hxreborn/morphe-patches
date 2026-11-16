/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.trailer

import app.morphe.patcher.Fingerprint

internal object SeriesUiModelFingerprint : Fingerprint(
    strings = listOf("MicroDramaSeriesUiModel(id="),
)

internal object SeriesUiModelConstructorFingerprint : Fingerprint(
    classFingerprint = SeriesUiModelFingerprint,
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "J",
        "J",
    ),
)
