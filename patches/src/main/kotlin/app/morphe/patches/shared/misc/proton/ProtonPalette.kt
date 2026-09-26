/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

internal const val CORE_COMPOSE_THEME_PACKAGE = "Lme/proton/core/compose/theme/"

internal object ProtonPalette {
    const val HAITI = 0xFF1B1340L
    const val VALHALLA = 0xFF271B54L
    const val JACARTA = 0xFF2E2260L
    const val CHAMBRAY = 0xFF372580L
    const val SAN_MARINO = 0xFF4D34B3L
    const val CORNFLOWER_BLUE = 0xFF6D4AFFL
    const val PORTAGE = 0xFF8A6EFFL
    const val PERANO = 0xFFC4B7FFL
    const val ENZIAN_BASE = 0xFF5252CCL
    const val PURPLE_BASE = 0xFF8080FFL
    const val BALTIC_SEA = 0xFF1C1B24L
}

internal val CORE_BRAND_COLORS = listOf(
    ProtonPalette.CHAMBRAY,
    ProtonPalette.SAN_MARINO,
    ProtonPalette.CORNFLOWER_BLUE,
    ProtonPalette.PORTAGE,
    ProtonPalette.PERANO,
    ProtonPalette.HAITI,
    ProtonPalette.VALHALLA,
    ProtonPalette.JACARTA,
    ProtonPalette.ENZIAN_BASE,
    ProtonPalette.PURPLE_BASE,
)

internal object BrandPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(ProtonPalette.CORNFLOWER_BLUE)),
)

internal fun MutableMethod.injectColorTransformCall(index: Int, method: String) {
    val register = getInstruction<OneRegisterInstruction>(index).registerA

    addInstructions(
        index + 1,
        """
            invoke-static/range { v$register .. v${register + 1} }, $method
            move-result-wide v$register
        """,
    )
}

private fun MutableMethod.injectAccentColorCalls(brandColors: Set<Long>): Set<Long> {
    val brandColorInstructions = implementation!!.instructions
        .withIndex()
        .filter { (_, instruction) -> (instruction as? WideLiteralInstruction)?.wideLiteral in brandColors }

    brandColorInstructions
        .map { it.index }
        .reversed()
        .forEach { index -> injectColorTransformCall(index, "$ACCENT_COLOR_CLASS->transformBrandColor(J)J") }

    return brandColorInstructions
        .map { (_, instruction) -> (instruction as WideLiteralInstruction).wideLiteral }
        .toSet()
}

internal fun BytecodePatchContext.transformBrandColors(colors: Collection<Long>) {
    val brandColors = colors.toSet()
    val transformedBrandColors = BrandPaletteFingerprint.matchAll()
        .flatMap { match -> match.method.injectAccentColorCalls(brandColors) }
        .toSet()

    val missingBrandColors = brandColors - transformedBrandColors
    if (missingBrandColors.isNotEmpty()) {
        throw PatchException(
            "No brand palette initializer defines: " +
                missingBrandColors.joinToString { "#%08X".format(it) },
        )
    }

    markFeaturePatched(ACCENT_COLOR_CLASS)
}
