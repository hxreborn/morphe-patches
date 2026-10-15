/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.viewmode

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/LibraryViewMenu;"

@Suppress("unused")
val libraryViewModesPatch = bytecodePatch(
    name = "Library view modes",
    description = "Adds the Full, Brief, Thumbnails and Grid views to book lists.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val viewModeClass = ViewModeEnumFingerprint.classDef
        val savedViewMode = viewModeClass.fields.single {
            it.type == viewModeClass.type && AccessFlags.STATIC.isSet(it.accessFlags) &&
                !AccessFlags.FINAL.isSet(it.accessFlags)
        }
        val viewModeItemId = viewModeClass.fields.single {
            it.type == "I" && !AccessFlags.STATIC.isSet(it.accessFlags)
        }

        CurrentViewModeFingerprint.let {
            val defaultModeIndex = it.instructionMatches[3].index
            val register = it.method.getInstruction<OneRegisterInstruction>(defaultModeIndex).registerA
            it.method.replaceInstruction(defaultModeIndex, "sget-object v$register, $savedViewMode")
        }

        LibraryOptionsMenuFingerprint.method.apply {
            val filterIndex = LibraryOptionsMenuFingerprint.instructionMatches.first().index
            val filterItemId = getInstruction<WideLiteralInstruction>(filterIndex).wideLiteral

            implementation!!.instructions.withIndex()
                .filter { (index, instruction) -> index > filterIndex && instruction.opcode == Opcode.RETURN_VOID }
                .map { it.index }
                .reversed()
                .forEach { returnIndex ->
                    val registers = getFreeRegisterProvider(returnIndex, 3)
                    val menuRegister = registers.getFreeRegister4Bit()
                    val filterIdRegister = registers.getFreeRegister4Bit()
                    val modeIdRegister = registers.getFreeRegister4Bit()

                    addInstructions(
                        returnIndex,
                        """
                            move-object/from16 v$menuRegister, p1
                            const v$filterIdRegister, $filterItemId
                            invoke-static { }, ${CurrentViewModeFingerprint.method}
                            move-result-object v$modeIdRegister
                            iget v$modeIdRegister, v$modeIdRegister, $viewModeItemId
                            invoke-static { v$menuRegister, v$filterIdRegister, v$modeIdRegister }, $EXTENSION_CLASS_DESCRIPTOR->show(Landroid/view/Menu;II)V
                        """,
                    )
                }
        }
    }
}
