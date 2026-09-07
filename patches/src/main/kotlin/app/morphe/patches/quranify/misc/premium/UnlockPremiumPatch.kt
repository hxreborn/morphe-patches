/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/quranify/PremiumUnlock;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks downloading every surah, lyrics and tafsir, Android Auto, " +
        "background playback controls, and insights.",
) {
    compatibleWith(AppCompatibilities.QURANIFY)
    extendWith("extensions/extension.mpe")

    execute {
        BuildCustomerInfoFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;)V",
        )
    }
}
