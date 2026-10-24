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
import app.morphe.patches.shared.misc.proton.PROTON_EXTENSION_PACKAGE
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "${PROTON_EXTENSION_PACKAGE}LoadingIndicator;"
private const val SHOW_METHOD = "$EXTENSION_CLASS->show(Ljava/lang/Object;ZF)V"
private const val IMAGE_MODIFIER_PARAMETER = 2
private const val IMAGE_COMPOSER_PARAMETER = 7
private const val ANDROID_VIEW_MODIFIER_DEFAULT_MASK = 0x2

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "Material 3 loading indicator",
    description = "Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(EXTENSION_CLASS)

        val pullingIconMatch = PullToRefreshPullingIconFingerprint.matchSingle()
        val pullingIconMethod = pullingIconMatch.method
        val rotateIndex = pullingIconMatch.instructionMatches[2].index
        val rotateCall = pullingIconMethod.getInstruction<FiveRegisterInstruction>(rotateIndex)
        val imageIndex = pullingIconMatch.instructionMatches.last().index
        val imageCall = pullingIconMethod.getInstruction<RegisterRangeInstruction>(imageIndex)
        val imageReference = imageCall.getReference<MethodReference>()!!
        val modifierType = imageReference.parameterTypes[IMAGE_MODIFIER_PARAMETER].toString()
        val composerType = imageReference.parameterTypes[IMAGE_COMPOSER_PARAMETER].toString()

        val androidView = androidViewFingerprint(modifierType, composerType).matchSingle().originalMethod
        val functionType = androidView.parameterTypes.first().toString()

        mutableClassDefBy(EXTENSION_CLASS).replaceStub(
            "show",
            10,
            """
                const-class v0, $functionType
                invoke-static { v0 }, $EXTENSION_CLASS->factory(Ljava/lang/Class;)Ljava/lang/Object;
                move-result-object v1
                check-cast v1, $functionType
                const/4 v2, 0x0
                invoke-static { v0, p1, p2 }, $EXTENSION_CLASS->update(Ljava/lang/Class;ZF)Ljava/lang/Object;
                move-result-object v3
                check-cast v3, $functionType
                move-object v4, p0
                check-cast v4, $composerType
                const/4 v5, 0x0
                const/4 v6, $ANDROID_VIEW_MODIFIER_DEFAULT_MASK
                invoke-static/range { v1 .. v6 }, ${descriptor(androidView)}
                return-void
            """,
        )

        PullToRefreshSpinnerFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 3)
            val composer = registers.getFreeRegister4Bit()
            val indeterminate = registers.getFreeRegister4Bit()
            val rotation = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS->isEnabled()Z
                    move-result v$indeterminate
                    if-eqz v$indeterminate, :original
                    move-object/from16 v$composer, p1
                    const/4 v$indeterminate, 0x1
                    const/4 v$rotation, 0x0
                    invoke-static { v$composer, v$indeterminate, v$rotation }, $SHOW_METHOD
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
                invoke-static { }, $EXTENSION_CLASS->isEnabled()Z
                move-result v$enabledRegister
                if-eqz v$enabledRegister, :image
                const/16 v${composerRegister + 1}, 0x0
                move/from16 v${composerRegister + 2}, v$rotationRegister
                invoke-static/range { v$composerRegister .. v${composerRegister + 2} }, $SHOW_METHOD
                goto :drawn
            """,
            ExternalLabel("image", pullingIconMethod.getInstruction(imageIndex)),
            ExternalLabel("drawn", pullingIconMethod.getInstruction(imageIndex + 1)),
        )
    }
}
