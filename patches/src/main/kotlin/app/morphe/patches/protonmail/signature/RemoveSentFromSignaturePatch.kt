/*
 * Copyright (C) 2025 Aoife McCullough
 * Copyright (C) 2026 hxreborn
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
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.protonmail.shared.ARM32
import app.morphe.patches.protonmail.shared.ARM64
import app.morphe.patches.protonmail.shared.MAIL_UNIFFI_LIBRARY
import app.morphe.patches.protonmail.shared.X86_64
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.appliedPatchMarkerPatch
import app.morphe.patches.shared.replaceAsciiInPlace
import app.morphe.patches.shared.replaceMasked

private const val DEFAULT_MOBILE_SIGNATURE =
    """Sent from <a target="_blank" href="https://proton.me/mail/home">Proton Mail</a> for Android."""

private class PaidAccountCallSite(pattern: String, mask: String, val callOffsets: List<Int>) {
    val pattern = pattern.hexToByteArray()
    val mask = mask.hexToByteArray()
}

private class PaidAccountCallSites(callReplacement: String, val sites: List<PaidAccountCallSite>) {
    val callReplacement = callReplacement.hexToByteArray()
}

private val PAID_ACCOUNT_CALL_SITES = mapOf(
    ARM64 to PaidAccountCallSites(
        callReplacement = "20008052",
        sites = listOf(
            PaidAccountCallSite(
                pattern = "000000940001003468664039091d0012080100523f090071e9079f1a2001080a0200001440008052",
                mask = "000000fcffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                callOffsets = listOf(0),
            ),
            PaidAccountCallSite(
                pattern = "000000947901003419000052e00318aa0000009480010036",
                mask = "000000fcffffffffffffffffffffffff000000fcffffffff",
                callOffsets = listOf(0, 16),
            ),
        ),
    ),
    ARM32 to PaidAccountCallSites(
        callReplacement = "012000bf",
        sites = listOf(
            PaidAccountCallSite(
                pattern = "10b50c4600f000d040b1607bc1b280f00100023918bf0121084010bd022010bd",
                mask = "ffffffff00f800d0ffffffffffffffffffffffffffffffffffffffffffffffff",
                callOffsets = listOf(4),
            ),
            PaidAccountCallSite(
                pattern = "384600f000d0189d1a9981b180f0010b384600f000d090b1687b",
                mask = "ffff00f800d0ffffffffffffffffffffffff00f800d0ffffffff",
                callOffsets = listOf(2, 18),
            ),
        ),
    ),
    X86_64 to PaidAccountCallSites(
        callReplacement = "b80100000090",
        sites = listOf(
            PaidAccountCallSite(
                pattern = "534889f3ff15000000008a4b1989ca80f20131f680f9020fb6ca0f44ce84c0b802000000",
                mask = "ffffffffffff00000000ffffffffffffffffffffffffffffffffffffffffffffffffffff",
                callOffsets = listOf(4),
            ),
            PaidAccountCallSite(
                pattern = "4889f7ff15000000004189c684db74224180f6014889efff150000000084c07427",
                mask = "ffffffffff00000000ffffffffffffffffffffffffffffffff00000000ffffffff",
                callOffsets = listOf(3, 23),
            ),
        ),
    ),
)

private val removeSentFromSignatureMarkerPatch =
    appliedPatchMarkerPatch("removeSentFromSignature")

@Suppress("unused")
val removeSentFromSignaturePatch = resourcePatch(
    name = "Remove 'Sent from' signature",
    description = "Removes the 'Sent from Proton Mail' signature and unlocks the mobile signature setting.",
) {
    dependsOn(patchesSettingsPatch, removeSentFromSignatureMarkerPatch)
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        val architectures = PAID_ACCOUNT_CALL_SITES.filterKeys { get("lib/$it/$MAIL_UNIFFI_LIBRARY").exists() }
        if (architectures.isEmpty()) throw PatchException("Could not find $MAIL_UNIFFI_LIBRARY for $ARM64, $ARM32 or $X86_64")

        for ((architecture, callSites) in architectures) {
            val library = get("lib/$architecture/$MAIL_UNIFFI_LIBRARY")

            if (!library.replaceAsciiInPlace(DEFAULT_MOBILE_SIGNATURE, " ".repeat(DEFAULT_MOBILE_SIGNATURE.length))) {
                throw PatchException("Could not find the default mobile signature in $architecture")
            }

            for (site in callSites.sites) {
                val replacements = site.callOffsets.associateWith { callSites.callReplacement }
                if (!library.replaceMasked(site.pattern, site.mask, replacements)) {
                    throw PatchException("Could not find the mobile signature paid account check in $architecture")
                }
            }
        }
    }
}
