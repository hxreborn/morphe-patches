/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonmail.misc.theme.webview.WebSettingsCreatedFingerprint
import app.morphe.patches.protonmail.misc.theme.webview.WebSettingsPageFinishedFingerprint
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/AccentColor;"

private fun MutableMethod.injectAccentColorCalls(): Set<Long> {
    val brandColors = implementation!!.instructions
        .withIndex()
        .filter { (_, instruction) -> (instruction as? WideLiteralInstruction)?.wideLiteral in BRAND_COLORS }

    brandColors
        .map { it.index }
        .reversed()
        .forEach { index -> injectColorTransformCall(index, "$EXTENSION_CLASS->applyAccentToArgb(J)J") }

    return brandColors
        .map { (_, instruction) -> (instruction as WideLiteralInstruction).wideLiteral }
        .toSet()
}

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val recolored = BrandPaletteFingerprint.matchAll()
            .flatMap { match -> match.method.injectAccentColorCalls() }
            .toSet()

        val missing = BRAND_COLORS - recolored
        if (missing.isNotEmpty()) {
            throw PatchException(
                "Brand colors missing from the app palette: " +
                    missing.joinToString { "#%08X".format(it) },
            )
        }

        WebSettingsCreatedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p2 }, $EXTENSION_CLASS->hideWebViewWithTimeout(Landroid/webkit/WebView;)V",
        )

        WebSettingsPageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->injectAccentStyles(Landroid/webkit/WebView;)V",
        )

        mutableClassDefBy(EXTENSION_CLASS).methods
            .single { it.name == "isPatched" }
            .returnEarly(true)
    }
}
