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
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE
import app.morphe.patches.shared.misc.proton.SettingsRowIcon
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val APPLICATION_CLASS = "Lcom/protonvpn/android/ProtonApplicationHilt;"
private const val ICON_PARAMETER = 1
private const val TITLE_PARAMETER = 2
private const val ON_CLICK_PARAMETER = 8

private fun BytecodePatchContext.addPatchesSettingsRow() {
    val iconId = getResourceId(ResourceType.DRAWABLE, "ic_proton_wrench")
        ?: throw PatchException("Missing settings icon: ic_proton_wrench")

    val match = WidgetSettingsRowFingerprint.matchSingle()
    val rowIndex = match.instructionMatches.last().index

    val settingsRowMethod = match.classDef.addSettingsRowMethod(
        match.method,
        rowIndex,
        TITLE_PARAMETER,
        ON_CLICK_PARAMETER,
        SettingsRowIcon(ICON_PARAMETER, iconId),
    )

    val row = match.method.getInstruction<RegisterRangeInstruction>(rowIndex)
    val composer = row.startRegister + row.getReference<MethodReference>()!!.parameterTypes.indexOf(COMPOSER_TYPE)
    match.method.addInstructions(rowIndex + 1, "invoke-static/range { v$composer .. v$composer }, $settingsRowMethod")
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(resourceMappingPatch, patchesSettingsActivityPatch("@style/ProtonTheme.Vpn.Mobile"))
    extendWith("extensions/extension.mpe")

    execute {
        injectBundleVersion()
        injectAppCompatDefaultNightMode()
        attachPatchContext(APPLICATION_CLASS)
        addPatchesSettingsRow()
    }
}
