/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.morphe.patches.shared.misc.proton.amoledBackgroundOverlayPatch
import app.morphe.patches.shared.misc.proton.transformCoreDarkBackgrounds
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val BACKGROUND_NORM = 41
private const val BACKGROUND_STRONG = 44
private const val ITEM_DETAIL_BACKGROUND = 65
private const val DARK_BACKGROUND_PALETTE_FIELD = "EerieBlack"
private val DARK_BACKGROUND_ROLES = listOf(BACKGROUND_NORM, BACKGROUND_STRONG, ITEM_DETAIL_BACKGROUND)
    .associateWith { DARK_BACKGROUND_PALETTE_FIELD }
private const val NIGHT_STYLES = "res/values-night/styles.xml"
private const val SPLASH_STYLE = "ProtonTheme.Splash.Pass"
private const val SPLASH_BACKGROUND_ITEM = "windowSplashScreenBackground"
private const val BLACK_RESOURCE = "@android:color/black"

private val amoledSplashBackgroundPatch = resourcePatch {
    execute {
        document(NIGHT_STYLES).use { document ->
            document.getElementsByTagName("style")
                .findElementByAttributeValueOrThrow("name", SPLASH_STYLE)
                .getElementsByTagName("item")
                .findElementByAttributeValueOrThrow("name", SPLASH_BACKGROUND_ITEM)
                .textContent = BLACK_RESOURCE
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch, amoledSplashBackgroundPatch, amoledBackgroundOverlayPatch)

    execute {
        markFeaturePatched(AMOLED_THEME_CLASS)
        transformCoreDarkBackgrounds()

        PassColorsInitializerFingerprint.method.transformPassColors(
            DARK_COLORS_FIELD,
            DARK_BACKGROUND_ROLES,
            "$AMOLED_THEME_CLASS->transformPackedBackground(J)J",
        )
    }
}
