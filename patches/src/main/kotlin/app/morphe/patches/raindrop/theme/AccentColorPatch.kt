/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/raindrop/AccentColor;"

private const val LIGHT_ACCENT_ENTRY_4_7 = "52 54 07 0C 07"
private const val LIGHT_ACCENT_ENTRY_TO_SILVER_4_7 = "52 64 08 0C 07"
private const val DARK_ACCENT_ENTRY_4_7 = "52 85 08 15 09"
private const val DARK_ACCENT_ENTRY_TO_MAROON_4_7 = "52 EA 07 15 09"
private const val LIGHT_ACCENT_ENTRY_4_8 = "52 86 07 3E 07"
private const val LIGHT_ACCENT_ENTRY_TO_SILVER_4_8 = "52 8D 08 3E 07"
private const val DARK_ACCENT_ENTRY_4_8 = "52 A4 08 3B 09"
private const val DARK_ACCENT_ENTRY_TO_MAROON_4_8 = "52 0F 08 3B 09"
private const val SILVER_KEYWORD_HEX = "23 63 30 63 30 63 30"
private const val LIGHT_PLACEHOLDER = "23 31 39 38 38 65 30"
private const val MAROON_KEYWORD_HEX = "23 38 30 30 30 30 30"
private const val DARK_PLACEHOLDER = "23 44 43 43 32 38 46"

private val HEX_COLOR = Regex("#(?:[0-9A-Fa-f]{2})?([0-9A-Fa-f]{6})")

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Accent color",
    description = "Replaces the accent color with the wallpaper color on Android 12 and later, or with a chosen " +
        "color. A new wallpaper color applies after the app restarts.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)
    extendWith("extensions/extension.mpe")

    dependsOn(
        hermesVariantsPatch(
            setOf(
                LIGHT_ACCENT_ENTRY_4_7 to LIGHT_ACCENT_ENTRY_TO_SILVER_4_7,
                DARK_ACCENT_ENTRY_4_7 to DARK_ACCENT_ENTRY_TO_MAROON_4_7,
                SILVER_KEYWORD_HEX to LIGHT_PLACEHOLDER,
                MAROON_KEYWORD_HEX to DARK_PLACEHOLDER,
            ),
            setOf(
                LIGHT_ACCENT_ENTRY_4_8 to LIGHT_ACCENT_ENTRY_TO_SILVER_4_8,
                DARK_ACCENT_ENTRY_4_8 to DARK_ACCENT_ENTRY_TO_MAROON_4_8,
                SILVER_KEYWORD_HEX to LIGHT_PLACEHOLDER,
                MAROON_KEYWORD_HEX to DARK_PLACEHOLDER,
            ),
        ),
    )

    val materialYou by booleanOption(
        key = "materialYou",
        default = true,
        title = "Material You",
        description = "Takes the accent from the wallpaper on Android 12 and later.",
        required = true,
    )

    val accentColor by stringOption(
        key = "accentColor",
        title = "Accent color",
        description = "Hex color such as #1988E0. Applies when Material You is off or unavailable. " +
            "Leave empty for the stock accent.",
        validator = { it.isNullOrBlank() || HEX_COLOR.matches(it.trim()) },
    )

    execute {
        val customColor = accentColor?.trim()?.takeIf { it.isNotEmpty() }?.let {
            "#" + HEX_COLOR.matchEntire(it)!!.groupValues[1].lowercase()
        }

        CreateBundleLoaderFingerprint.matchSingle().apply {
            val callIndex = instructionMatches.last().index
            val call = method.getInstruction<FiveRegisterInstruction>(callIndex)
            val companionRegister = call.registerC
            val contextRegister = call.registerD
            val urlRegister = call.registerE
            val loaderRegister = method.getInstruction<OneRegisterInstruction>(callIndex + 1).registerA
            val insertIndex = callIndex + 2

            val registers = method.getFreeRegisterProvider(
                insertIndex,
                3,
                companionRegister,
                contextRegister,
                urlRegister,
                loaderRegister,
            )
            val materialYouRegister = registers.getFreeRegister4Bit()
            val colorRegister = registers.getFreeRegister4Bit()
            val pathRegister = registers.getFreeRegister4Bit()
            val loadColor = customColor?.let { "const-string v$colorRegister, \"$it\"" }
                ?: "const/4 v$colorRegister, 0x0"

            method.addInstructionsWithLabels(
                insertIndex,
                """
                    const/4 v$materialYouRegister, ${if (materialYou == true) "0x1" else "0x0"}
                    $loadColor
                    invoke-static { v$contextRegister, v$urlRegister, v$materialYouRegister, v$colorRegister }, $EXTENSION_CLASS->recoloredBundlePath(Landroid/content/Context;Ljava/lang/String;ZLjava/lang/String;)Ljava/lang/String;
                    move-result-object v$pathRegister
                    if-eqz v$pathRegister, :stock
                    invoke-virtual { v$companionRegister, v$pathRegister }, $JS_BUNDLE_LOADER_COMPANION->createFileLoader(Ljava/lang/String;)Lcom/facebook/react/bridge/JSBundleLoader;
                    move-result-object v$loaderRegister
                """,
                ExternalLabel("stock", method.getInstruction(insertIndex)),
            )
        }
    }
}
