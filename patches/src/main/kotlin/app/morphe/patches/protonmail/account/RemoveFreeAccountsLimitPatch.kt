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

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.protonmail.shared.ARM32
import app.morphe.patches.protonmail.shared.ARM64
import app.morphe.patches.protonmail.shared.RUST_CORE
import app.morphe.patches.protonmail.shared.X86_64
import app.morphe.patches.protonmail.shared.replaceTrailingMasked
import app.morphe.patches.shared.compat.AppCompatibilities

private fun hex(value: String): ByteArray {
    require(value.length % 2 == 0) { "Hex pattern needs an even number of digits" }

    return ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

private class LimitCheck(pattern: String, mask: String, bypass: String) {
    val pattern = hex(pattern)
    val mask = hex(mask)
    val bypass = hex(bypass)
}

private val LIMIT_CHECKS = mapOf(
    ARM64 to LimitCheck(
        pattern = "090540f93f0118ebc9000054",
        mask = "00fcffff1ffce0ffffffffff",
        bypass = "1f2003d5",
    ),
    ARM32 to LimitCheck(
        pattern = "d0e902013a1a76eb010249d3",
        mask = "ffffffffffffffffffffffff",
        bypass = "49e0",
    ),
    X86_64 to LimitCheck(
        pattern = "488b50084839ea761e",
        mask = "ffffffffffffffffff",
        bypass = "6690",
    ),
)

@Suppress("unused")
val removeFreeAccountsLimitPatch = resourcePatch(
    name = "Remove free accounts limit",
    description = "Removes the limit for maximum free accounts logged in.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        var foundNativeCore = false

        for ((architecture, check) in LIMIT_CHECKS) {
            val nativeCore = get("lib/$architecture/$RUST_CORE")
            if (!nativeCore.exists()) continue

            foundNativeCore = true

            if (!nativeCore.replaceTrailingMasked(check.pattern, check.mask, check.bypass)) {
                throw PatchException("Could not find the free accounts limit check for $architecture")
            }
        }

        if (!foundNativeCore) {
            throw PatchException("Found no $RUST_CORE to patch")
        }
    }
}
