/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.loadingindicator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.LOADING_INDICATOR_CLASS
import app.morphe.patches.shared.misc.proton.SHOW_LOADING_INDICATOR
import app.morphe.patches.shared.misc.proton.androidViewFingerprint
import app.morphe.patches.shared.misc.proton.bindLoadingIndicatorView
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val IMAGE_MODIFIER_PARAMETER = 2
private const val IMAGE_COMPOSER_PARAMETER = 7
private const val ANDROID_VIEW_PARAMETERS = 6

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "Material 3 loading indicator",
    description = "Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(LOADING_INDICATOR_CLASS)

        val pullingIconMatch = PullToRefreshPullingIconFingerprint.matchSingle()
        val pullingIconMethod = pullingIconMatch.method
        val rotateIndex = pullingIconMatch.instructionMatches[2].index
        val rotateCall = pullingIconMethod.getInstruction<FiveRegisterInstruction>(rotateIndex)
        val imageIndex = pullingIconMatch.instructionMatches.last().index
        val imageCall = pullingIconMethod.getInstruction<RegisterRangeInstruction>(imageIndex)
        val imageReference = imageCall.getReference<MethodReference>()!!
        val modifierType = imageReference.parameterTypes[IMAGE_MODIFIER_PARAMETER].toString()
        val composerType = imageReference.parameterTypes[IMAGE_COMPOSER_PARAMETER].toString()

        val androidView = androidViewFingerprint(modifierType, composerType, ANDROID_VIEW_PARAMETERS)
            .matchSingle().originalMethod
        bindLoadingIndicatorView(androidView, null)

        PullToRefreshSpinnerFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 3)
            val composer = registers.getFreeRegister4Bit()
            val indeterminate = registers.getFreeRegister4Bit()
            val rotation = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $LOADING_INDICATOR_CLASS->isEnabled()Z
                    move-result v$indeterminate
                    if-eqz v$indeterminate, :original
                    move-object/from16 v$composer, p1
                    const/4 v$indeterminate, 0x1
                    const/4 v$rotation, 0x0
                    invoke-static { v$composer, v$indeterminate, v$rotation }, $SHOW_LOADING_INDICATOR
                    return-void
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }

        val composerRegister = imageCall.startRegister + IMAGE_COMPOSER_PARAMETER
        val rotationRegister = rotateCall.registerD
        val enabledRegister = pullingIconMethod.getFreeRegisterProvider(imageIndex, 1, rotationRegister).getFreeRegister()
        pullingIconMethod.addInstructionsWithLabels(
            imageIndex,
            """
                invoke-static { }, $LOADING_INDICATOR_CLASS->isEnabled()Z
                move-result v$enabledRegister
                if-eqz v$enabledRegister, :image
                const/16 v${composerRegister + 1}, 0x0
                move/from16 v${composerRegister + 2}, v$rotationRegister
                invoke-static/range { v$composerRegister .. v${composerRegister + 2} }, $SHOW_LOADING_INDICATOR
                goto :drawn
            """,
            ExternalLabel("image", pullingIconMethod.getInstruction(imageIndex)),
            ExternalLabel("drawn", pullingIconMethod.getInstruction(imageIndex + 1)),
        )
    }
}
