/*
 * SPDX-FileCopyrightText: 2025 ByteEVM
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/49ae0df224f42bc511d3e4d748c6b94f6273b44c
 * Commit 49ae0df224f42bc511d3e4d748c6b94f6273b44c (2025-05-30),
 * patches/src/main/kotlin/app/revanced/patches/protonmail/account/RemoveFreeAccountsLimitPatch.kt
 */
package app.morphe.patches.protonmail.account

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

@Suppress("unused")
val removeFreeAccountsLimitPatch = resourcePatch(
    name = "Remove free accounts limit",
    description = "Removes the limit for maximum free accounts logged in.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        document("res/values/integers.xml").use { document ->
            document.documentElement.childNodes.findElementByAttributeValueOrThrow(
                "name",
                "core_feature_auth_user_check_max_free_user_count",
            ).textContent = Int.MAX_VALUE.toString()
        }
    }
}
