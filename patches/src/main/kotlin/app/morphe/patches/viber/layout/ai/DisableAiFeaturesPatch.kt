/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/AiPreferences;"

@Suppress("unused")
val disableAiFeaturesPatch: BytecodePatch = bytecodePatch(
    name = "Disable AI features",
    description = "Removes AI reply suggestions, chat summaries and image remix controls.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("disableAiFeatures", disableAiFeaturesPatch)
        ImageRemixIntroductionFingerprint.matchSingle().method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        )
        ImageRemixPresetsFingerprint.matchSingle().apply {
            val (_, _, availability) = instructionMatches
            val register = (availability.instruction as OneRegisterInstruction).registerA
            method.addInstructions(
                availability.index + 1,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->showImageRemix(Z)Z
                    move-result v$register
                """,
            )
        }
        AppPreferenceValueFingerprint.matchSingle().apply {
            val (returnValue) = instructionMatches
            val valueRegister = (returnValue.instruction as OneRegisterInstruction).registerA
            val keyRegister = method.implementation!!.registerCount
            val (expanded, parameterMoves) = method.addFreeRegisters(1)
            expanded.addInstructions(
                returnValue.index + parameterMoves,
                """
                    invoke-virtual { p0 }, ${classDef.type}->getKey()Ljava/lang/String;
                    move-result-object v$keyRegister
                    invoke-static { v$keyRegister, v$valueRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideValue(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$valueRegister
                """,
            )
        }
        val (_, constructor) = AiReplyPreferencesFingerprint.matchSingle().instructionMatches
        val preferenceType = constructor.instruction.getReference<MethodReference>()!!.definingClass
        booleanPreferenceValueFingerprint(preferenceType).matchSingle().apply {
            val (keyRead, _, returnValue) = instructionMatches
            val keyField = keyRead.instruction.getReference<FieldReference>()!!
            val valueRegister = (returnValue.instruction as OneRegisterInstruction).registerA
            val keyRegister = method.implementation!!.registerCount
            val (expanded, parameterMoves) = method.addFreeRegisters(1)
            expanded.addInstructions(
                returnValue.index + parameterMoves,
                """
                    iget-object v$keyRegister, p0, $keyField
                    invoke-static { v$keyRegister, v$valueRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideBoolean(Ljava/lang/String;Z)Z
                    move-result v$valueRegister
                """,
            )
        }
    }
}
