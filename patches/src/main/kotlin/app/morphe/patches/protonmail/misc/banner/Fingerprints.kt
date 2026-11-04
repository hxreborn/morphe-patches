/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.banner

import app.morphe.patcher.Fingerprint

internal object AutoDeleteStatesFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("AutoDeleteUpsell", "AutoDeleteDisabled", "AutoDeleteEnabled"),
)

internal object AutoDeleteBannerFingerprint : Fingerprint(
    strings = listOf("Getting the auto-delete banner failed."),
)
