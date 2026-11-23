/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.hermes

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.all.misc.hermes.HERMES_BUNDLE_PATH
import app.morphe.util.byteArrayOf
import java.security.MessageDigest

private const val SHA1_LENGTH = 20

internal fun hermesVariantsPatch(vararg variants: Set<Pair<String, String>>) = patch {
    execute {
        val file = resourceFile(HERMES_BUNDLE_PATH, true)
        val bundle = file.readBytes()
        val bundleText = bundle.toString(Charsets.ISO_8859_1)

        val replacements = variants.firstOrNull { variant ->
            variant.all { (pattern, _) -> pattern.hexToLatin1() in bundleText }
        } ?: throw PatchException("No replacement set matches every pattern in $HERMES_BUNDLE_PATH")

        replacements.forEach { (pattern, replacement) ->
            byteArrayOf(replacement).copyInto(bundle, bundleText.indexOf(pattern.hexToLatin1()))
        }

        val footerStart = bundle.size - SHA1_LENGTH
        MessageDigest.getInstance("SHA-1")
            .apply { update(bundle, 0, footerStart) }
            .digest()
            .copyInto(bundle, footerStart)
        file.writeBytes(bundle)
    }
}

private fun String.hexToLatin1() = byteArrayOf(this).toString(Charsets.ISO_8859_1)
