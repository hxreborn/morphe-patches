/*
 * SPDX-FileCopyrightText: 2025 Aoife McCullough
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/8ed9d5bf087d8392e945d471c2a42b52a393ceaf
 * Commit 8ed9d5bf087d8392e945d471c2a42b52a393ceaf (2025-04-02),
 * patches/src/main/kotlin/app/revanced/patches/protonmail/signature/RemoveSentFromSignaturePatch.kt
 */
package app.morphe.patches.protonmail.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.protonmail.shared.RUST_CORE
import app.morphe.patches.protonmail.shared.replaceAsciiInPlace
import app.morphe.patches.shared.compat.AppCompatibilities

private const val DEFAULT_SIGNATURE =
    """Sent from <a target="_blank" href="https://proton.me/mail/home">Proton Mail</a> for Android."""

private val COMMENTED_OUT_SIGNATURE = "<!--".padEnd(DEFAULT_SIGNATURE.length - 3) + "-->"

@Suppress("unused")
val removeSentFromSignaturePatch = resourcePatch(
    name = "Remove 'Sent from' signature",
    description = "Removes the 'Sent from Proton Mail' signature from emails.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        val nativeCores = get("lib").walk().filter { it.name == RUST_CORE }

        if (nativeCores.count { it.replaceAsciiInPlace(DEFAULT_SIGNATURE, COMMENTED_OUT_SIGNATURE) } == 0) {
            throw PatchException("Could not find the default mobile signature")
        }
    }
}
