/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.materialswitch

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.protonmail.misc.theme.webview.WebSettingsPageFinishedFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.MATERIAL_SWITCHES_CLASS
import app.morphe.patches.shared.misc.proton.drawSwitchAsView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/WebMaterialSwitch;"
private const val COMPOSER_PARAMETER = 4

private fun BytecodePatchContext.drawNativeSwitchesAsViews() {
    val materialSwitch = MaterialSwitchFingerprint.matchSingle().originalMethod
    val switch = switchWrapperFingerprint(materialSwitch).matchSingle().method

    drawSwitchAsView(switch, COMPOSER_PARAMETER, null, ::androidViewFingerprint)
}

@Suppress("unused")
val materialSwitchesPatch = bytecodePatch(
    name = "Material 3 switches",
    description = "Shows switches in the Material 3 style with check and close icons.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(MATERIAL_SWITCHES_CLASS)
        WebSettingsPageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->apply(Landroid/webkit/WebView;)V",
        )
        drawNativeSwitchesAsViews()
    }
}
