/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.etsy.ads

import app.morphe.patcher.Fingerprint

internal object SearchGridBoxFingerprint : Fingerprint(
    strings = listOf("Layout type not supported: "),
)

internal object SearchHeaderFingerprint : Fingerprint(
    strings = listOf("buildProcessVideoFilterPill should not be called for CONTROL"),
)
