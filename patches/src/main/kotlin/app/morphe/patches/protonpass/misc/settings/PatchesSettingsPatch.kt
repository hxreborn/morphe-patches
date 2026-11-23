/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.PASS_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.protonPassSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedNightModeExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedProxyHandlerExtensionPatch

private const val HOST_ACTIVITY = "me.proton.core.devicemigration.presentation.DeviceMigrationActivity"
private const val APPLICATION_CLASS = "Lproton/android/pass/App;"
private const val TITLE_PARAMETER = 1
private const val ON_CLICK_PARAMETER = 5

private fun addPatchesSettingsRow() {
    val match = ApplicationSectionFingerprint.match()
    val (optionIndex, dividerIndex) = match.instructionMatches.let { it[1].index to it[2].index }

    val settingsRowMethod = match.classDef.addSettingsRowMethod(
        match.method,
        optionIndex,
        TITLE_PARAMETER,
        ON_CLICK_PARAMETER,
    )

    with(match.method) {
        val divider = getInstruction<FiveRegisterInstruction>(dividerIndex)
        val dividerReference = divider.getReference<MethodReference>()!!
        val dividerRegisters = listOf(divider.registerC, divider.registerD, divider.registerE, divider.registerF, divider.registerG)
            .take(divider.registerCount)
        val composer = dividerRegisters[dividerReference.parameterTypes.indexOf(COMPOSER_TYPE)]

        addInstructions(
            dividerIndex + 1,
            """
                invoke-static/range { v$composer .. v$composer }, $settingsRowMethod
                invoke-static { ${dividerRegisters.joinToString { "v$it" }} }, ${descriptor(dividerReference)}
            """,
        )
    }
}

internal val patchesSettingsPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonPassSettingsExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedNightModeExtensionPatch, sharedProxyHandlerExtensionPatch)
    dependsOn(patchesSettingsActivityPatch("@style/ProtonTheme.Pass", HOST_ACTIVITY, PASS_SETTINGS_CLASS))
    execute {
        injectBundleVersion()
        injectAppCompatDefaultNightMode()
        attachPatchContext(APPLICATION_CLASS)
        addPatchesSettingsRow()
    }
}
