/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val APPLICATION_CLASS = "Lproton/android/pass/App;"
private const val TITLE_PARAMETER = 1
private const val ON_CLICK_PARAMETER = 5

private fun BytecodePatchContext.addPatchesSettingsRow() {
    val match = ApplicationSectionFingerprint.matchSingle()
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
                invoke-static { ${dividerRegisters.joinToString { "v$it" }} }, ${DexFormatter.INSTANCE.getMethodDescriptor(dividerReference)}
            """,
        )
    }
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(resourceMappingPatch, patchesSettingsActivityPatch("@style/ProtonTheme.Pass"))
    extendWith("extensions/extension.mpe")

    execute {
        injectBundleVersion()
        injectAppCompatDefaultNightMode()
        attachPatchContext(APPLICATION_CLASS)
        addPatchesSettingsRow()
    }
}
