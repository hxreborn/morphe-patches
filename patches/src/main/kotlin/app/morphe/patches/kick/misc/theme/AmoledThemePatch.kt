/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.kick.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.replaceAsciiInPlace
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val JAVASCRIPT_BUNDLE = "assets/index.android.bundle"
private const val WEB_ASSETS = "assets/public"
private const val CAPACITOR_CONFIG = "assets/capacitor.config.json"
private const val DARK_BACKGROUND = "#090A0B"
private const val WEB_DARK_BACKGROUND = "#0b0e0f"
private const val WEB_DARK_BACKGROUND_RGB = "rgb(11 14 15"
private const val BLACK = "#000000"
private const val BLACK_RGB = "rgb(00 00 00"
private const val TV_BLACK = 0xFF000000L
private const val UPDATES_ENABLED = "expo.modules.updates.ENABLED"
private const val AUTO_UPDATE_ENABLED = "\"autoUpdate\": true"
private const val AUTO_UPDATE_DISABLED = "\"autoUpdate\": false"
private val WEB_ASSET_EXTENSIONS = setOf("css", "js")

private var replacedBackground = false

private val bundledAssetsThemePatch = resourcePatch {
    execute {
        replacedBackground = false

        val javascriptBundle = get(JAVASCRIPT_BUNDLE)

        if (javascriptBundle.exists()) {
            if (!javascriptBundle.replaceAsciiInPlace(DARK_BACKGROUND, BLACK)) {
                throw PatchException("Could not find the dark theme background in the JavaScript bundle")
            }

            document("AndroidManifest.xml").use { document ->
                document.getElementsByTagName("meta-data")
                    .findElementByAttributeValueOrThrow("android:name", UPDATES_ENABLED)
                    .setAttribute("android:value", "false")
            }

            replacedBackground = true

            return@execute
        }

        val webAssets = get(WEB_ASSETS)

        if (!webAssets.isDirectory) return@execute

        val replaced = webAssets.walkTopDown()
            .filter { it.isFile && it.extension in WEB_ASSET_EXTENSIONS }
            .fold(false) { patched, file ->
                val background = file.replaceAsciiInPlace(WEB_DARK_BACKGROUND, BLACK)
                val backgroundRgb = file.replaceAsciiInPlace(WEB_DARK_BACKGROUND_RGB, BLACK_RGB)

                patched || background || backgroundRgb
            }

        if (!replaced) {
            throw PatchException("Could not find the dark theme background in the web assets")
        }

        get(CAPACITOR_CONFIG).apply {
            writeText(readText().replace(AUTO_UPDATE_ENABLED, AUTO_UPDATE_DISABLED))
        }

        replacedBackground = true
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black. " +
        "Disables over-the-air updates that would restore the original background.",
) {
    compatibleWith(AppCompatibilities.KICK)
    dependsOn(bundledAssetsThemePatch)

    execute {
        val palette = TvPaletteFingerprint.matchAll(0..1).singleOrNull()

        if (palette == null) {
            if (!replacedBackground) {
                throw PatchException("Could not find the dark theme background")
            }

            return@execute
        }

        palette.method.apply {
            val index = indexOfFirstLiteralInstructionOrThrow(TV_DARK_BACKGROUND)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            replaceInstruction(index, "const-wide v$register, 0x${TV_BLACK.toString(16)}L")
        }
    }
}
