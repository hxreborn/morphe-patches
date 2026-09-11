/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.telemetry

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly

private const val SENTRY_ANDROID_CLASS = "Lio/sentry/android/core/SentryAndroid;"

private val INIT_PARAMETERS = listOf(
    "Landroid/content/Context;",
    "Lio/sentry/ILogger;",
    "Lio/sentry/Sentry\$OptionsConfiguration;",
)

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Stops crash and error reports from reaching Sentry.",
) {
    compatibleWith(AppCompatibilities.RISE)

    execute {
        mutableClassDefBy(SENTRY_ANDROID_CLASS)
            .methods.single {
                it.name == "init" && it.parameters.map(CharSequence::toString) == INIT_PARAMETERS
            }
            .returnEarly()
    }
}
