/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.Fingerprint

internal object SidebarUpsellingFeatureFlagFingerprint : Fingerprint(
    strings = listOf("MailAndroidUpsellingSidebar"),
)
