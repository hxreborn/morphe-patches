/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.settings.returnWhenEnabled
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/aliexpress/PatchSettings;"
private const val DARK_THEME_CLASS = "Lapp/hxreborn/extension/aliexpress/DarkTheme;"
private const val LIGHT_THEME_PARENT_PREFIX = "@android:style/Theme.Material.Light"
private const val LIGHT_MARKER = ".Light"

private val themeResourcesPatch = resourcePatch {
    execute {
        decode("res/layout*", "res/drawable*", "res/color*", "res/values*/styles.xml", "res/values*/colors.xml")
        DarkResources(get("res")).rewrite()
        document("res/values/styles.xml").use { base ->
            document("res/values-night/styles.xml").use { night ->
                val styles = base.getElementsByTagName("style")
                val lightStyles = (0 until styles.length).map { styles.item(it) }.filter {
                    it.attributes.getNamedItem("parent")?.nodeValue?.startsWith(LIGHT_THEME_PARENT_PREFIX) == true
                }
                lightStyles.forEach { style ->
                    val darkStyle = night.importNode(style, true)
                    val parent = darkStyle.attributes.getNamedItem("parent")
                    parent.nodeValue = parent.nodeValue.replace(LIGHT_MARKER, "")
                    night.documentElement.appendChild(darkStyle)
                }
            }
        }
    }
}

private const val DESCRIPTION = "Fixes the screens and web pages the built-in Dark mode leaves white. Requires Android 10 or later."

@Suppress("unused")
val darkThemePatch = bytecodePatch(
    name = "Fix dark mode",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)
    dependsOn(spoofSignaturePatch, themeResourcesPatch)

    execute {
        markPatchApplied("darkTheme", DESCRIPTION)
        ActivityConfigurationFingerprint.matchSingle().method.returnWhenEnabled("$DARK_THEME_CLASS->followsDarkMode()Z")
        DefaultNightModeFingerprint.matchSingle().method.returnWhenEnabled("$DARK_THEME_CLASS->followsDarkMode()Z")
        DarkModeEnabledFingerprint.matchSingle().let { match ->
            val (deviceSupportsDark, serverAllowsDark) = match.instructionMatches
            val darkModeEnabled = match.method
            darkModeEnabled.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $PATCH_SETTINGS_CLASS->darkTheme()Z
                    move-result v0
                    if-eqz v0, :original
                    invoke-virtual { p0 }, ${darkModeEnabled.getInstruction<ReferenceInstruction>(deviceSupportsDark.index).reference}
                    move-result v0
                    if-eqz v0, :record
                    invoke-static { }, ${darkModeEnabled.getInstruction<ReferenceInstruction>(serverAllowsDark.index).reference}
                    move-result v0
                    :record
                    invoke-static { v0 }, $DARK_THEME_CLASS->recordHostDarkMode(Z)V
                    invoke-static { }, $DARK_THEME_CLASS->nativeSwitchOn()Z
                    move-result v0
                    return v0
                """.trimIndent(),
                ExternalLabel("original", darkModeEnabled.getInstruction(0)),
            )
        }
        val nativeSwitch = NativeDarkSwitchFingerprint.matchSingle().method
        nativeSwitch.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $PATCH_SETTINGS_CLASS->darkTheme()Z
                move-result v0
                if-eqz v0, :original
                invoke-static { }, $DARK_THEME_CLASS->nativeSwitchOn()Z
                move-result v0
                return v0
            """.trimIndent(),
            ExternalLabel("original", nativeSwitch.getInstruction(0)),
        )
        NavigationBarFillerFingerprint.matchSingle().let { match ->
            val (_, setBackground) = match.instructionMatches
            val fill = match.method.getInstruction<FiveRegisterInstruction>(setBackground.index)
            match.method.replaceInstruction(
                setBackground.index,
                "invoke-static { v${fill.registerC}, v${fill.registerD} }, $DARK_THEME_CLASS->fillNavigationBar(Landroid/view/View;I)V",
            )
        }
        TemplateColorFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p0, p2, p3 }, $DARK_THEME_CLASS->templateColor(Ljava/lang/Object;II)I
                move-result p3
            """.trimIndent(),
        )
        WebPageAttachFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p0 }, $DARK_THEME_CLASS->coverWebPage(Landroid/view/View;)V",
        )
        WebPageMessageFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p0, p1 }, $DARK_THEME_CLASS->revealWebPage(Landroid/view/View;Landroid/os/Message;)V",
        )
        BackdropImageFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p0, p2 }, $DARK_THEME_CLASS->dimBackdropImage(Ljava/lang/Object;Landroid/view/View;)V",
        )
        SearchBackdropImageFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p0, p2 }, $DARK_THEME_CLASS->dimSearchBackdropImage(Ljava/lang/Object;Landroid/view/View;)V",
        )
        QuantityControlFingerprint.matchSingle().let { match ->
            val (returnVoid) = match.instructionMatches
            match.method.addInstruction(
                returnVoid.index,
                "invoke-static { p0 }, $DARK_THEME_CLASS->darkenQuantityControl(Landroid/widget/LinearLayout;)V",
            )
        }
        mutableClassDefBy(PATCH_SETTINGS_CLASS).replaceStub(
            "initializeNightMode",
            1,
            """
                const/4 v0, -0x1
                invoke-static { v0 }, $SET_DEFAULT_NIGHT_MODE
                return-void
            """.trimIndent(),
        )
    }
}
