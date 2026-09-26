/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/splittunneling/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.splittunneling

import app.morphe.patcher.Fingerprint
import app.morphe.patches.protonvpn.misc.restrictions.RestrictionGuardFingerprint

internal object SplitTunnelingViewStateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/redesign/settings/ui/SettingsViewModel\$SettingViewState\$SplitTunneling;",
    name = "<init>",
    parameters = listOf(
        "Z",
        "Lcom/protonvpn/android/settings/data/SplitTunnelingMode;",
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Z",
        "I",
    ),
)

internal object SplitTunnelingRestrictionFingerprint : RestrictionGuardFingerprint("getSplitTunneling")
