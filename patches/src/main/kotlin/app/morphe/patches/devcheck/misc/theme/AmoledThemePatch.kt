/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.theme

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.devcheck.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.adoptChild
import app.morphe.util.getNode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/devcheck/AmoledTheme;"
private const val OVERLAY_STYLE = "hx_amoled_theme_overlay"
private val SET_CONTENT_VIEW_OVERLOADS = 3..3

private val PAGE_BACKGROUND_ITEMS = listOf(
    "android:windowBackground",
    "colorAppBackground",
    "rankings_surface_bg",
)

private val amoledThemeResourcesPatch = resourcePatch {
    execute {
        document("res/values/styles.xml").use { document ->
            document.getNode("resources").adoptChild("style") {
                setAttribute("name", OVERLAY_STYLE)
                PAGE_BACKGROUND_ITEMS.forEach { name ->
                    adoptChild("item") {
                        setAttribute("name", name)
                        textContent = "@android:color/black"
                    }
                }
            }
        }
        document("res/values/strings.xml").use { document ->
            document.getNode("resources").adoptChild("string") {
                setAttribute("name", "hx_amoled_preference_title")
                textContent = "Prefer pure black in dark theme"
            }
        }
        document("res/values/ids.xml").use { document ->
            document.getNode("resources").adoptChild("item") {
                setAttribute("name", "hx_amoled_preference")
                setAttribute("type", "id")
            }
        }
    }
}

private fun Match.useDialogThemePicker() {
    val (_, sdkRead, minimumSdk) = instructionMatches
    val register = (sdkRead.instruction as OneRegisterInstruction).registerA
    val minimum = (minimumSdk.instruction as WideLiteralInstruction).wideLiteral
    method.replaceInstruction(sdkRead.index, "const/16 v$register, $minimum")
}

@Suppress("unused")
val amoledThemePatch: BytecodePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Adds a pure black option to the dark theme.",
) {
    compatibleWith(AppCompatibilities.DEVCHECK.excluding("6.62"))
    dependsOn(unlockPremiumPatch, amoledThemeResourcesPatch)
    availability(requireArm64)
    extendWith("extensions/extension.mpe")

    execute {
        ThemesMenuFingerprint.matchSingle().useDialogThemePicker()
        val dialog = ThemesDialogFingerprint.matchSingle()
        dialog.useDialogThemePicker()
        val (_, _, _, _, show) = dialog.instructionMatches
        val call = dialog.method.getInstruction<FiveRegisterInstruction>(show.index)
        dialog.method.addInstructions(
            show.index + 1,
            "invoke-static { v${call.registerC} }, $EXTENSION_CLASS->addPreference(Landroid/app/Dialog;)V",
        )
        contentViewFingerprint(dialog.classDef.superclass!!).matchAll(SET_CONTENT_VIEW_OVERLOADS).forEach { match ->
            match.method.addInstructions(
                0,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->applyOverlay(Landroid/app/Activity;)V",
            )
        }
    }
}
