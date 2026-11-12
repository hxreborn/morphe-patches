/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val TTS_UPSELL_ROW = 0
internal const val TTS_FOOTNOTE_ROW = 1
internal const val TTS_SKIP_HEADERS_ROW = 2

private const val SWITCH_TYPE = "Landroidx/appcompat/widget/SwitchCompat;"
private const val SET_VISIBILITY = "Landroid/view/View;->setVisibility(I)V"

internal class TtsPreference(val flag: FieldReference, val settingsGetter: MethodReference)

internal fun BytecodePatchContext.bindTtsPreference(row: Int): TtsPreference {
    val prefsClass = TtsPrefsEventFingerprint.classDef.type
    val view = TtsPrefsViewFingerprint
    val rowRegister = view.instructionMatches[row].getInstruction<FiveRegisterInstruction>().registerC
    val instructions = view.method.implementation!!.instructions.toList()
    val switchField = instructions.indices.firstNotNullOf { index ->
        val field = instructions[index].takeIf { it.opcode == Opcode.IPUT_OBJECT }
            ?.getReference<FieldReference>()?.takeIf { it.type == SWITCH_TYPE }
        val lookup = instructions.getOrNull(index - 3) as? FiveRegisterInstruction
        field.takeIf {
            lookup?.opcode == Opcode.INVOKE_VIRTUAL &&
                lookup.getReference<MethodReference>()?.name == "findViewById" &&
                lookup.registerC == rowRegister
        }
    }

    val toggle = ttsSwitchToggleFingerprint(prefsClass, switchField)
    val flag = toggle.instructionMatches[0].instruction.getReference<FieldReference>()!!
    val setter = toggle.instructionMatches[2].instruction.getReference<MethodReference>()!!
    val settingsGetter = toggle.method.getInstruction(toggle.instructionMatches[0].index - 2)
        .getReference<MethodReference>()!!
    check(settingsGetter.returnType == flag.definingClass) { "Settings getter not found before $flag" }

    val setterMatch = ttsFlagSetterFingerprint(setter.definingClass, setter.name)
    val preference = setterMatch.instructionMatches[2].instruction.getReference<FieldReference>()!!
    val sharedPreferences = setterMatch.method.getInstruction(setterMatch.instructionMatches[0].index - 1)
        .getReference<FieldReference>()!!

    ttsFlagInitFingerprint(flag.definingClass, flag).let { init ->
        init.method.apply {
            val readPreference = implementation!!.instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.let { method ->
                        method.definingClass == preference.type && method.returnType == "Ljava/lang/Boolean;"
                    } == true
            }.getReference<MethodReference>()!!
            val storeIndex = init.instructionMatches.single().index
            val store = getInstruction<TwoRegisterInstruction>(storeIndex)
            val registers = getFreeRegisterProvider(storeIndex, 2, store.registerA, store.registerB)
            val valueRegister = registers.getFreeRegister4Bit()
            val preferencesRegister = registers.getFreeRegister4Bit()
            replaceInstruction(storeIndex, "iput-boolean v$valueRegister, v${store.registerB}, $flag")
            addInstructions(
                storeIndex,
                """
                    sget-object v$valueRegister, $preference
                    sget-object v$preferencesRegister, $sharedPreferences
                    invoke-virtual { v$valueRegister, v$preferencesRegister }, $readPreference
                    move-result-object v$valueRegister
                    invoke-virtual { v$valueRegister }, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v$valueRegister
                """,
            )
        }
    }

    return TtsPreference(flag, settingsGetter)
}

internal fun BytecodePatchContext.showTtsPreferenceRow(row: Int) = copyTtsPreferenceRowVisibility(row, TTS_UPSELL_ROW)

internal fun BytecodePatchContext.hideTtsPreferenceRow(row: Int) = copyTtsPreferenceRowVisibility(row, TTS_FOOTNOTE_ROW)

private fun BytecodePatchContext.copyTtsPreferenceRowVisibility(row: Int, sourceRow: Int) {
    val view = TtsPrefsViewFingerprint
    val rowRegister = view.instructionMatches[row].getInstruction<FiveRegisterInstruction>().registerC
    val visibilityRegister = view.instructionMatches[sourceRow].getInstruction<FiveRegisterInstruction>().registerD
    view.method.replaceInstruction(
        view.instructionMatches[row].index,
        "invoke-virtual { v$rowRegister, v$visibilityRegister }, $SET_VISIBILITY",
    )
}
