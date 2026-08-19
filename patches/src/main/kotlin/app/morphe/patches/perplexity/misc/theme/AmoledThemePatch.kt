/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val ALPHA_MASK = 0xFF000000L
private const val RGB_MASK = 0x00FFFFFFL
private const val OPAQUE = 0xFFL
private const val BACKGROUND_CHANNEL = 0x17L
private const val TINT_CHANNEL = 0xD6L
private const val BLACK = "#000000"
private const val SPLASH_COLOR_NAME = "background"

private fun MutableMethod.replaceColorWithBlack(color: Long) {
    val index = indexOfFirstLiteralInstructionOrThrow(color)
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    val black = color and ALPHA_MASK

    replaceInstruction(index, "const-wide v$register, 0x${black.toString(16)}L")
}

private fun MutableMethod.matchTintToBlack(color: Long) {
    val index = indexOfFirstLiteralInstructionOrThrow(color)
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    val alpha = color ushr 24
    val blended = BACKGROUND_CHANNEL + (TINT_CHANNEL - BACKGROUND_CHANNEL) * alpha / OPAQUE
    val matched = (blended * OPAQUE + TINT_CHANNEL / 2) / TINT_CHANNEL
    val tint = (matched shl 24) or (color and RGB_MASK)

    replaceInstruction(index, "const v$register, 0x${tint.toString(16)}")
}

private val splashBackgroundPatch = resourcePatch {
    execute {
        document("res/values/colors.xml").use { document ->
            document.getElementsByTagName("color")
                .findElementByAttributeValueOrThrow("name", SPLASH_COLOR_NAME)
                .textContent = BLACK
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PERPLEXITY)
    dependsOn(splashBackgroundPatch)

    execute {
        DarkColorSchemeFingerprint.method.apply {
            DARK_BACKGROUND_COLORS.forEach(::replaceColorWithBlack)
            TRANSLUCENT_SURFACE_COLORS.forEach(::matchTintToBlack)
        }
    }
}
