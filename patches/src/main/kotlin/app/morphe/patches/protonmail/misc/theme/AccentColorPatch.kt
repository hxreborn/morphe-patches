/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonmail.misc.theme.webview.webSettingsThemePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/AccentColor;"

private fun MutableMethod.injectAccentColorCalls(): Set<Long> {
    val brandColorInstructions = implementation!!.instructions
        .withIndex()
        .filter { (_, instruction) -> (instruction as? WideLiteralInstruction)?.wideLiteral in BRAND_COLORS }

    brandColorInstructions
        .map { it.index }
        .reversed()
        .forEach { index -> injectColorTransformCall(index, "$EXTENSION_CLASS->applyAccentToArgb(J)J") }

    return brandColorInstructions
        .map { (_, instruction) -> (instruction as WideLiteralInstruction).wideLiteral }
        .toSet()
}

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(webSettingsThemePatch)

    execute {
        val recoloredBrandColors = BrandPaletteFingerprint.matchAll()
            .flatMap { match -> match.method.injectAccentColorCalls() }
            .toSet()

        val missingBrandColors = BRAND_COLORS - recoloredBrandColors
        if (missingBrandColors.isNotEmpty()) {
            throw PatchException(
                "Brand colors missing from the app palette: " +
                    missingBrandColors.joinToString { "#%08X".format(it) },
            )
        }

        mutableClassDefBy(EXTENSION_CLASS).methods
            .single { it.name == "isPatched" }
            .returnEarly(true)
    }
}
