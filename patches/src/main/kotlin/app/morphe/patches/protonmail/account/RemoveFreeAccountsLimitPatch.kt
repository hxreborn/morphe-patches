/*
 * Copyright (C) 2025 ByteEVM
 * Copyright (C) 2026 hxreborn
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
import app.morphe.patches.protonmail.misc.fix.signature.spoofSignaturePatch
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
        pattern = "090140b93f05007181000054090540f93f0118ebc9000054",
        mask = "00fcffbf1ffcff7f0f0000ff00fcffff1ffce0ff0f0000ff",
        bypass = "1f2003d5",
    ),
    ARM32 to LimitCheck(
        pattern = "d0e902013a1a76eb0102",
        mask = "ffffffffffffffffffff",
        bypass = "0022013a",
    ),
    X86_64 to LimitCheck(
        pattern = "488b43408338017500488b5008",
        mask = "ffff00ffff38ffff00ffffffff",
        bypass = "4883caff",
    ),
)

@Suppress("unused")
val removeFreeAccountsLimitPatch = resourcePatch(
    name = "Remove free accounts limit",
    description = "Removes the limit for maximum free accounts logged in.",
) {
    dependsOn(spoofSignaturePatch)
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        var patchedArchitectures = 0

        for ((architecture, check) in LIMIT_CHECKS) {
            val nativeCore = get("lib/$architecture/$RUST_CORE")
            if (!nativeCore.exists()) continue

            if (nativeCore.replaceTrailingMasked(check.pattern, check.mask, check.bypass)) {
                patchedArchitectures++
            }
        }

        if (patchedArchitectures == 0) {
            throw PatchException("Could not find the free accounts limit check in any $RUST_CORE")
        }
    }
}
