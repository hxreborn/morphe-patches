/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.pagecounter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

internal object ChapterPageCounterFingerprint : Fingerprint(
    definingClass = "Lorg/readera/read/widget/ReadLayoutMain;",
    filters = listOf(literal(1000L)),
)
