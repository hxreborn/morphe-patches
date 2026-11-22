/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.shared.misc.proton.SettingsRowIcon
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.VPN_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import java.util.logging.Logger
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.protonVpnSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedNightModeExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedProxyHandlerExtensionPatch

private const val HOST_ACTIVITY = "me.proton.core.devicemigration.presentation.DeviceMigrationActivity"
private const val APPLICATION_CLASS = "Lcom/protonvpn/android/ProtonApplicationHilt;"
private const val ICON_PARAMETER = 1
private const val TITLE_PARAMETER = 2
private const val ON_CLICK_PARAMETER = 8
private const val COMPOSER_PARAMETER = 9

private val logger = Logger.getLogger("app.morphe.patches.protonvpn.misc.settings")

private fun addPatchesSettingsRow() {
    val iconId = resourceId(ResourceType.DRAWABLE, "ic_proton_wrench")

    val match = WidgetSettingsRowFingerprint.match()
    val rowIndex = match.instructionMatches.last().index

    val settingsRowMethod = match.classDef.addSettingsRowMethod(
        match.method,
        rowIndex,
        TITLE_PARAMETER,
        ON_CLICK_PARAMETER,
        SettingsRowIcon(ICON_PARAMETER, iconId),
        COMPOSER_PARAMETER,
    )

    val row = match.method.getInstruction<RegisterRangeInstruction>(rowIndex)
    val composer = row.startRegister + COMPOSER_PARAMETER
    match.method.addInstructions(rowIndex + 1, "invoke-static/range { v$composer .. v$composer }, $settingsRowMethod")
}

internal val patchesSettingsPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, protonVpnSettingsExtensionPatch, sharedNightModeExtensionPatch, sharedProxyHandlerExtensionPatch)
    dependsOn(resourceFieldsPatch, patchesSettingsActivityPatch("@style/ProtonTheme.Vpn.Mobile", HOST_ACTIVITY, VPN_SETTINGS_CLASS))
    execute {
        injectBundleVersion()
        attachPatchContext(APPLICATION_CLASS)
        try {
            injectAppCompatDefaultNightMode()
            addPatchesSettingsRow()
        } catch (ex: PatchException) {
            logger.warning("Skipped the patches settings row: ${ex.message}")
        }
    }
}
