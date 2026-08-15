/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.symfonium.misc.premium

import app.morphe.patcher.Fingerprint

internal object LicenseKeyCheckFingerprint : Fingerprint(
    strings = listOf("S2V5Q2hlY2s="),
)

internal object NativeVerdictHandlerFingerprint : Fingerprint(
    definingClass = "Lapp/symfonik/init/HandlerInitializable\$handler\$1;",
    name = "init",
)
