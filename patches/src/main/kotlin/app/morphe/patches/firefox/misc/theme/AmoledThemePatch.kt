/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.childElementsSequence
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION = "Lapp/hxreborn/extension/firefox/AmoledTheme;"
private const val PAGE_COLOR = "$EXTENSION->pageColor(Ljava/lang/Object;)J"
private const val CARD_COLOR = "$EXTENSION->cardColor(Ljava/lang/Object;)J"
private const val COLOR_PREFIX = "fx_mobile_"
private const val OLED_NIGHT_COLORS_PATH = "res/values-vrheadset-night/colors.xml"
private const val OLED_COLOR_PREFIX = "fx_mobile_oled_"

private val HOMEPAGE_NIGHT_COLORS = mapOf(
    "homepage_edge_to_edge_gradient_start" to "@color/fx_mobile_oled_background",
    "homepage_edge_to_edge_gradient_end" to "@color/fx_mobile_oled_background",
    "homepage_tab_edge_to_edge_tab_strip_item_background_active" to "@color/fx_mobile_oled_surface_container_highest",
    "homepage_tab_edge_to_edge_tab_strip_item_background_inactive" to "@color/fx_mobile_oled_surface_container_high",
)

private val oledNightColorsPatch = resourcePatch {
    execute {
        val defaultColors = document("res/values/colors.xml").use { document ->
            document.documentElement.childElementsSequence()
                .filter { it.tagName == "color" }
                .associate { it.getAttribute("name") to it.textContent }
        }
        val oledColors = defaultColors.keys.filter {
            it.startsWith(OLED_COLOR_PREFIX) && COLOR_PREFIX + it.removePrefix(OLED_COLOR_PREFIX) in defaultColors
        }
        if (oledColors.isEmpty()) throw PatchException("No $OLED_COLOR_PREFIX colors found")

        val nightColors = document("res/values-night/colors.xml").use { document ->
            document.documentElement.childElementsSequence()
                .filter { it.tagName == "color" }
                .map { it.getAttribute("name") }
                .toSet()
        }
        HOMEPAGE_NIGHT_COLORS.keys.forEach { name ->
            if (name !in nightColors) throw PatchException("Night color $name not found")
        }

        val entries = oledColors.associate { COLOR_PREFIX + it.removePrefix(OLED_COLOR_PREFIX) to "@color/$it" } +
            HOMEPAGE_NIGHT_COLORS
        val colors = get(OLED_NIGHT_COLORS_PATH, copy = false)
        colors.parentFile?.mkdirs()
        colors.writeText(
            entries.entries.joinToString(
                separator = "\n",
                prefix = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n",
                postfix = "\n</resources>\n",
            ) { (name, value) -> "    <color name=\"$name\">$value</color>" },
        )
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    dependsOn(oledNightColorsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("amoledTheme")

        ThemeValuesFingerprint.matchSingle().let { match ->
            val (_, _, darkStore, _, oledInit) = match.instructionMatches
            val darkField = darkStore.instruction.getReference<FieldReference>()!!
            val oledRegister = (oledInit.instruction as FiveRegisterInstruction).registerC
            val method = match.method
            val index = oledInit.index + 1
            val scratch = method.getFreeRegisterProvider(index, 1, oledRegister).getFreeRegister()
            method.addInstructionsWithLabels(
                index,
                """
                    invoke-static { }, $PATCH_SETTINGS->amoledTheme()Z
                    move-result v$scratch
                    if-eqz v$scratch, :stock
                    sput-object v$oledRegister, $darkField
                """,
                ExternalLabel("stock", method.getInstruction(index)),
            )
        }
        listOf(
            reportBrokenSitePageColorFingerprints to PAGE_COLOR,
            reportBrokenSiteCardColorFingerprints to CARD_COLOR,
        ).forEach { (fingerprints, colorCall) ->
            fingerprints.forEach { fingerprint ->
                fingerprint.matchSingle().let { match ->
                    val read = match.instructionMatches.first()
                    val instruction = read.instruction as TwoRegisterInstruction
                    match.method.replaceInstruction(read.index, "invoke-static { v${instruction.registerB} }, $colorCall")
                    match.method.addInstruction(read.index + 1, "move-result-wide v${instruction.registerA}")
                }
            }
        }
        ApplyOverrideConfigurationFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION->oledConfiguration(Landroid/content/res/Configuration;)V",
        )
    }
}
