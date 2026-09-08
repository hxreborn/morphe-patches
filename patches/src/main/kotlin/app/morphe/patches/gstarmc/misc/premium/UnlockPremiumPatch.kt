/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.gstarmc.misc.jiagu.bundledResource
import app.morphe.patches.gstarmc.misc.jiagu.jiaguRuntimePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.jiagu.asJiaguDex
import java.security.MessageDigest

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks the paid drawing, annotation and measurement tools, and removes ads.",
) {
    compatibleWith(AppCompatibilities.DWG_FASTVIEW)
    dependsOn(jiaguRuntimePatch)

    execute {
        val replacements = String(bundledResource("parts.txt")).trim().lines().associate { line ->
            val (index, digest) = line.split(' ')
            index.toInt() to digest
        }

        val classes = get("classes.dex")
        val packed = classes.readBytes().asJiaguDex()

        val parts = packed.parts().mapIndexed { index, part ->
            val expected = replacements[index] ?: return@mapIndexed part

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(part.cipherText)
                .joinToString("") { "%02x".format(it) }

            if (digest != expected) {
                throw PatchException(
                    "Packed dex $index is $digest, but this patch replaces $expected. " +
                        "The app version does not match the one the bundle was built for.",
                )
            }

            part.withCipherText(bundledResource("part$index.bin"))
        }

        classes.writeBytes(packed.withParts(parts))
    }
}
