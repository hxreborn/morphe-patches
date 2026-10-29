/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.morphe.patches.shared.misc.proton.amoledBackgroundOverlayPatch
import app.morphe.patches.shared.misc.proton.indexOfLastWrite
import app.morphe.patches.shared.misc.proton.injectColorTransformCall
import app.morphe.patches.shared.misc.proton.transformCoreDarkBackgrounds
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.adoptChild
import app.morphe.util.findElementByAttributeValue
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getNode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val SPLASH_STYLE = "ProtonTheme.Splash.Vpn.DarkPreferred"
private const val SPLASH_BACKGROUND_ITEM = "windowSplashScreenBackground"
private const val SPLASH_BACKGROUND_COLOR = "hx_splash_background"
private const val SPLASH_BACKGROUND_COLOR_V31 = "hx_splash_background_v31"
private val SPLASH_BACKGROUND_VALUES = mapOf(
    SPLASH_BACKGROUND_COLOR to mapOf(
        "res/values/colors.xml" to "@color/baltic_sea",
        "res/values-night/colors.xml" to "@android:color/black",
    ),
    SPLASH_BACKGROUND_COLOR_V31 to mapOf(
        "res/values/colors.xml" to "@color/background_norm",
        "res/values-night/colors.xml" to "@android:color/black",
    ),
)
private val SPLASH_STYLE_BACKGROUNDS = mapOf(
    "res/values/styles.xml" to SPLASH_BACKGROUND_COLOR,
    "res/values-v31/styles.xml" to SPLASH_BACKGROUND_COLOR_V31,
)

private val amoledSplashBackgroundPatch = resourcePatch {
    execute {
        SPLASH_BACKGROUND_VALUES.forEach { (color, values) ->
            values.forEach { (file, value) ->
                document(file).use { document ->
                    document.getNode("resources").adoptChild("color") {
                        setAttribute("name", color)
                        textContent = value
                    }
                }
            }
        }

        SPLASH_STYLE_BACKGROUNDS.forEach { (file, color) ->
            document(file).use { document ->
                val style = document.getElementsByTagName("style").findElementByAttributeValueOrThrow("name", SPLASH_STYLE)
                val background = "@color/$color"
                val item = style.getElementsByTagName("item").findElementByAttributeValue("name", SPLASH_BACKGROUND_ITEM)
                if (item != null) {
                    item.textContent = background
                } else {
                    style.adoptChild("item") {
                        setAttribute("name", SPLASH_BACKGROUND_ITEM)
                        textContent = background
                    }
                }
            }
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black and can dim menus, bars and buttons.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, amoledBackgroundOverlayPatch, amoledSplashBackgroundPatch)

    execute {
        markFeaturePatched(AMOLED_THEME_CLASS)
        transformCoreDarkBackgrounds()

        CollapsingToolbarColorFingerprint.matchSingle().run {
            val lerpIndex = instructionMatches.last().index
            val collapsedColorRegister = method.getInstruction<FiveRegisterInstruction>(lerpIndex).registerE
            method.injectColorTransformCall(
                method.indexOfLastWrite(collapsedColorRegister, lerpIndex),
                "$AMOLED_THEME_CLASS->transformPackedBackground(J)J",
            )
        }
    }
}
