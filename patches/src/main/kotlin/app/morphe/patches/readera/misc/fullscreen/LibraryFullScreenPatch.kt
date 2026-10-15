/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.fullscreen

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val libraryFullScreenPatch = bytecodePatch(
    name = "Library full screen",
    description = "Adds the option to use full-screen mode in the library as well as while reading.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val (readingModePreference, libraryModePreference) = SaveFullScreenModeFingerprint.instructionMatches
            .slice(1..2)
            .map { it.instruction.getReference<FieldReference>()!! }

        fullScreenFlagsInitFingerprint(
            SaveFullScreenModeFingerprint.classDef.type,
            readingModePreference,
        ).let { flags ->
            val (_, sharedPreferencesMatch, readMatch, _, defaultValueMatch) = flags.instructionMatches
            val sharedPreferences = sharedPreferencesMatch.instruction.getReference<FieldReference>()!!
            val readPreference = readMatch.instruction.getReference<MethodReference>()!!
            val defaultValueIndex = defaultValueMatch.index
            flags.method.apply {
                val flagRegister = getInstruction<OneRegisterInstruction>(defaultValueIndex).registerA
                val preferencesRegister = getFreeRegisterProvider(defaultValueIndex, 1, flagRegister).getFreeRegister4Bit()

                addInstruction(defaultValueIndex + 2, "const/4 v$flagRegister, 0x0")
                replaceInstruction(defaultValueIndex, "sget-object v$flagRegister, $libraryModePreference")
                addInstructions(
                    defaultValueIndex + 1,
                    """
                        sget-object v$preferencesRegister, $sharedPreferences
                        invoke-virtual { v$flagRegister, v$preferencesRegister }, $readPreference
                        move-result-object v$flagRegister
                        invoke-virtual { v$flagRegister }, Ljava/lang/Boolean;->booleanValue()Z
                        move-result v$flagRegister
                    """,
                )
            }
        }

        FullScreenPreferencesFingerprint.let {
            val (readingVisibilityIndex, _, libraryVisibilityIndex) = it.instructionMatches.map { match -> match.index }
            it.method.apply {
                listOf(readingVisibilityIndex to 0, libraryVisibilityIndex to 1).forEach { (index, visible) ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    replaceInstruction(index, "const/4 v$register, $visible")
                }
            }
        }

        RootPreferencesFingerprint.let {
            it.method.addInstruction(
                it.instructionMatches.last().index + 1,
                "invoke-direct { p0 }, ${FullScreenPreferencesFingerprint.method}",
            )
        }
    }
}
