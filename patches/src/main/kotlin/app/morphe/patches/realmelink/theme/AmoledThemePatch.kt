/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val WEB_SETTINGS_CLASS = "Landroid/webkit/WebSettings;"
private const val TITLE_VIEW_CLASS = "Lcom/realme/iot/common/widgets/TitleView;"
private const val MODE_NIGHT_YES = 2
private const val BASE_WEB_VIEW_SETTINGS_SITES = 4
private const val STORE_WEB_VIEW_SETTINGS_SITES = 3
private val HOME_HEADER_TINT_SITES = 2..2
private val LIGHT_SYSTEM_BARS_SETTERS = 2..2
private const val HOME_BACKGROUND_COLOR = "#000000"
private const val OPAQUE_WHITE_ARGB = "-0x1"
private const val OPAQUE_BLACK_ARGB = "-0x1000000"

private const val BACK_ICON_NAME = "verify_sys_color_actionbar_back_white"

@Suppress("unused")
val amoledThemePatch = patch(
    name = "AMOLED dark theme",
    description = "Replaces the light theme with a pure black dark theme. " +
        "Requires Android 13 or later.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    dependsOn(amoledThemeResourcesPatch)

    execute {
        CommonTitleBarBackIconFingerprint.match().apply {
            val index = instructionMatches.single().index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA
            val backIcon = resourceId(ResourceType.DRAWABLE, BACK_ICON_NAME)

            method.replaceInstruction(index, "const v$register, 0x${backIcon.toString(16)}")
        }

        val setDefaultNightMode = SetDefaultNightModeFingerprint.method
        setDefaultNightMode.addInstructions(0, "const/4 p0, 0x${MODE_NIGHT_YES.toString(16)}")

        SetAppearanceLightSystemBarsFingerprint.matchAll(LIGHT_SYSTEM_BARS_SETTERS).forEach { match ->
            match.method.addInstructions(0, "const/4 p1, 0x0")
        }

        DefaultHomeBackgroundColorFingerprint.match().apply {
            val (colorString, _) = instructionMatches
            val index = colorString.index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA

            method.replaceInstruction(index, "const-string v$register, \"$HOME_BACKGROUND_COLOR\"")
        }

        mapOf(
            HeadsetSubScreenBackgroundFingerprint to OPAQUE_BLACK_ARGB,
            SpatialAudioPlayButtonTextColorFingerprint to OPAQUE_WHITE_ARGB,
        ).forEach { (fingerprint, color) ->
            fingerprint.match().apply {
                val (colorLiteral, _) = instructionMatches
                val index = colorLiteral.index
                val register = method.getInstruction<OneRegisterInstruction>(index).registerA

                method.replaceInstruction(index, "const v$register, $color")
            }
        }

        listOf(
            HeadsetSubScreenTitleFingerprint,
            HeadsetVirtualDeviceTitleFingerprint,
        ).forEach { fingerprint ->
            fingerprint.match().apply {
                val index = instructionMatches.single().index
                val titleRegister = method.getInstruction<FiveRegisterInstruction>(index).registerC
                val backgroundRegister = method.getFreeRegisterProvider(index + 1, 1, titleRegister)
                    .getFreeRegister4Bit()

                method.addInstructions(
                    index + 1,
                    """
                        const/4 v$backgroundRegister, 0x0
                        invoke-virtual { v$titleRegister, v$backgroundRegister }, $TITLE_VIEW_CLASS->setBackground(Landroid/graphics/drawable/Drawable;)V
                    """,
                )
            }
        }

        val hasStoreWebViews = StoreWebViewSettingsFingerprint.matchAllOrNull() != null
        val webViewSettingsSiteCount = BASE_WEB_VIEW_SETTINGS_SITES +
            if (hasStoreWebViews) STORE_WEB_VIEW_SETTINGS_SITES else 0

        WebViewSettingsFingerprint.matchAll(webViewSettingsSiteCount..webViewSettingsSiteCount).forEach { match ->
            match.method.apply {
                val (_, settingsResult) = match.instructionMatches
                val settingsIndex = settingsResult.index
                val settingsRegister = getInstruction<OneRegisterInstruction>(settingsIndex).registerA
                val allowedRegister = getFreeRegisterProvider(settingsIndex + 1, 1, settingsRegister)
                    .getFreeRegister4Bit()

                addInstructions(
                    settingsIndex + 1,
                    """
                        const/4 v$allowedRegister, 0x1
                        invoke-virtual { v$settingsRegister, v$allowedRegister }, $WEB_SETTINGS_CLASS->setAlgorithmicDarkeningAllowed(Z)V
                    """,
                )
            }
        }

        HomeHeaderTintFingerprint.matchAll(HOME_HEADER_TINT_SITES).forEach { match ->
            val (_, colorLiteral) = match.instructionMatches
            val index = colorLiteral.index
            val register = match.method.getInstruction<OneRegisterInstruction>(index).registerA

            match.method.replaceInstruction(index, "const v$register, $OPAQUE_WHITE_ARGB")
        }

        ApplicationOnCreateFingerprint.method.apply {
            val modeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    const/4 v$modeRegister, 0x${MODE_NIGHT_YES.toString(16)}
                    invoke-static { v$modeRegister }, ${setDefaultNightMode.definingClass}->${setDefaultNightMode.name}(I)V
                """,
            )
        }
    }
}
