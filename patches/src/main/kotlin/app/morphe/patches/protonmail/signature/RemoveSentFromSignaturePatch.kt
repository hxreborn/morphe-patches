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
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValue

@Suppress("unused")
val removeSentFromSignaturePatch = resourcePatch(
    name = "Remove 'Sent from' signature",
    description = "Removes the 'Sent from Proton Mail mobile' signature from emails.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    execute {
        val resourceDirectory = get("res")

        val blankedStrings = resourceDirectory.walk()
            .filter { it.isFile && it.name.equals("strings.xml", ignoreCase = true) }
            .count { stringsFile ->
                val path = "res/${stringsFile.relativeTo(resourceDirectory).invariantSeparatorsPath}"

                document(path).use { document ->
                    // Not localized in all languages
                    document.documentElement.childNodes.findElementByAttributeValue(
                        "name",
                        "mail_settings_identity_mobile_footer_default_free",
                    )?.apply { textContent = "" } != null
                }
            }

        if (blankedStrings == 0) throw PatchException("Could not find 'sent from' string in resources")
    }
}
