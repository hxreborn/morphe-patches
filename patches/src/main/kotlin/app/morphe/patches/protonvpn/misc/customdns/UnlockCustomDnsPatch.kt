/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ef804d67c346ee40f6430b80962896df4f9b880e
 * Commit ef804d67c346ee40f6430b80962896df4f9b880e (2026-09-02),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/customdns/UnlockCustomDnsPatch.kt
 */
package app.morphe.patches.protonvpn.misc.customdns

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.unlockUserSetting
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockCustomDnsPatch = bytecodePatch(
    name = "Unlock custom DNS",
    description = "Unlocks custom DNS on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)

    execute {
        unlockUserSetting(
            CustomDnsViewStateFingerprint,
            freeUserParameter = 4,
            CustomDnsRestrictionFingerprint,
        )
    }
}
