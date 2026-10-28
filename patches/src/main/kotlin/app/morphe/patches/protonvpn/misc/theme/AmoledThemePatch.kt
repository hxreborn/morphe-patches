/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.morphe.patches.shared.misc.proton.amoledBackgroundOverlayPatch
import app.morphe.patches.shared.misc.proton.indexOfLastWrite
import app.morphe.patches.shared.misc.proton.injectColorTransformCall
import app.morphe.patches.shared.misc.proton.transformCoreDarkBackgrounds
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black and can dim menus, bars and buttons.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, amoledBackgroundOverlayPatch)

    execute {
        markFeaturePatched(AMOLED_THEME_CLASS)
        transformCoreDarkBackgrounds()

        CollapsingToolbarColorFingerprint.matchSingle().run {
            val lerpIndex = instructionMatches.last().index
            val collapsedColorRegister = method.getInstruction<FiveRegisterInstruction>(lerpIndex).registerE
            method.injectColorTransformCall(
                method.indexOfLastWrite(collapsedColorRegister, lerpIndex),
                "$AMOLED_THEME_CLASS->transformPackedBackground(J)J",
            )
        }
    }
}
