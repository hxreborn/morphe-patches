/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.ACCENT_COLOR_CLASS
import app.morphe.patches.shared.misc.proton.CORE_BRAND_COLORS
import app.morphe.patches.shared.misc.proton.accentOverlayPatch
import app.morphe.patches.shared.misc.proton.transformBrandColors
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private object AuthenticatorPalette {
    const val HELIOTROPE = 0xFFB080FFL
    const val MAUVE = 0xFFCAAAFFL
    const val MEDIUM_PURPLE = 0xFFA779FFL
    const val ELECTRIC_VIOLET = 0xFF7350FFL
    const val DAISY_BUSH = 0xFF453099L
}

private val AUTHENTICATOR_BRAND_COLORS = listOf(
    AuthenticatorPalette.HELIOTROPE,
    AuthenticatorPalette.MAUVE,
    AuthenticatorPalette.MEDIUM_PURPLE,
    AuthenticatorPalette.ELECTRIC_VIOLET,
    AuthenticatorPalette.DAISY_BUSH,
)

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch, accentOverlayPatch)

    execute {
        transformBrandColors(CORE_BRAND_COLORS + AUTHENTICATOR_BRAND_COLORS)

        ButtonShadowColorFingerprint.matchSingle().let { match ->
            val shadowColor = match.instructionMatches.first()
            val register = shadowColor.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                shadowColor.index + 1,
                """
                    invoke-static { v$register }, $ACCENT_COLOR_CLASS->transformBrandColorArgb(I)I
                    move-result v$register
                """,
            )
        }
    }
}
