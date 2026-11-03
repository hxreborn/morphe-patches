/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/misc/rampup/UnlimitedRampupTimePatch.kt
 */
package app.morphe.patches.duolingo.misc.rampup

import app.morphe.patcher.Fingerprint

private const val RAMPUP_TIMER_STATE_CLASS = "Lcom/duolingo/data/session/state/RampupTimerState"

internal val rampupTimerStateConstructorFingerprints = listOf("Paused", "Tick").map { state ->
    Fingerprint(
        definingClass = "$RAMPUP_TIMER_STATE_CLASS\$$state;",
        name = "<init>",
    )
}
