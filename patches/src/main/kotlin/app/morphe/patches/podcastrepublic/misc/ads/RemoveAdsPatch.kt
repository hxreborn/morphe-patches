/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.podcastrepublic.misc.ads

import app.morphe.patcher.Match
import app.morphe.patcher.Match.InstructionMatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DEX_TRUE = "0x1"

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes in-app ads and frees up banner space.",
) {
    compatibleWith(AppCompatibilities.PODCAST_REPUBLIC)

    execute {
        val license = AdLicenseFingerprint.match()
        val (_, _, licenseReadResult) = license.instructionMatches
        license.setResultTrue(licenseReadResult)

        val bannerVisibility = bannerVisibilityFingerprint(license.method.definingClass).match()
        val (_, _, _, noAdsResult) = bannerVisibility.instructionMatches
        bannerVisibility.setResultTrue(noAdsResult)
    }
}

private fun Match.setResultTrue(result: InstructionMatch) {
    val register = result.getInstruction<OneRegisterInstruction>().registerA
    method.addInstruction(result.index + 1, "const/16 v$register, $DEX_TRUE")
}
