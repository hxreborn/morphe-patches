/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val ALPHA_MASK = 0xFF000000L
private const val RGB_MASK = 0x00FFFFFFL
private const val OPAQUE = 0xFFL
private const val CHANNEL_MASK = 0xFFL
private const val BLACK = "#000000"
private const val SPLASH_COLOR_NAME = "background"

private fun MutableMethod.replaceColorWithBlack(color: Long) {
    val index = indexOfFirstLiteralInstructionOrThrow(color)
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    val black = color and ALPHA_MASK

    replaceInstruction(index, "const-wide v$register, 0x${black.toString(16)}L")
}

private fun MutableMethod.matchTintToBlack(color: Long, palette: DarkPalette) {
    val index = indexOfFirstLiteralInstructionOrThrow(color)
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    val stockAlpha = color ushr 24
    val backgroundChannel = palette.background and CHANNEL_MASK
    val tintChannel = palette.tint and CHANNEL_MASK
    val blended = backgroundChannel + (tintChannel - backgroundChannel) * stockAlpha / OPAQUE
    val alphaOverBlack = (blended * OPAQUE + tintChannel / 2) / tintChannel
    val tint = (alphaOverBlack shl 24) or (color and RGB_MASK)

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
        val palette = darkPalettes.firstOrNull { it.fingerprint.methodOrNull != null }
            ?: throw PatchException("No known dark color palette matched")

        palette.fingerprint.matchSingle().method.apply {
            palette.backgrounds.forEach(::replaceColorWithBlack)
            palette.tints.forEach { matchTintToBlack(it, palette) }
        }
    }
}
