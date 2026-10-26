/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.shared.misc.proton.SettingsRowIcon
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import java.util.logging.Logger

private const val HOST_ACTIVITY = "me.proton.core.devicemigration.presentation.DeviceMigrationActivity"
private const val APPLICATION_CLASS = "Lcom/protonvpn/android/ProtonApplicationHilt;"
private const val ICON_PARAMETER = 1
private const val TITLE_PARAMETER = 2
private const val ON_CLICK_PARAMETER = 8
private const val COMPOSER_PARAMETER = 9

private val logger = Logger.getLogger("app.morphe.patches.protonvpn.misc.settings")

private fun BytecodePatchContext.addPatchesSettingsRow() {
    val iconId = resourceId(ResourceType.DRAWABLE, "ic_proton_wrench")

    val match = WidgetSettingsRowFingerprint.matchSingle()
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

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(resourceFieldsPatch, patchesSettingsActivityPatch("@style/ProtonTheme.Vpn.Mobile", HOST_ACTIVITY))
    extendWith("extensions/extension.mpe")

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
