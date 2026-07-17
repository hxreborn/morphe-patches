/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/2e1e79aff3c4d09d8264ca418ca684e32063552e
 * Commit 2e1e79aff3c4d09d8264ca418ca684e32063552e (2026-07-16),
 * patches/src/main/kotlin/app/revanced/patches/forus/misc/premium/Fingerprints.kt
 */
package app.morphe.patches.forus.misc.premium

import app.morphe.patcher.Fingerprint

// TODO: 2026-07-17 verify fingerprints

internal object PremiumGetterFingerprint : Fingerprint(
    strings = listOf("isPremium"),
)
