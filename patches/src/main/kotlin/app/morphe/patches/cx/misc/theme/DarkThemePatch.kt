/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.cx.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findMutableMethodOf
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/cx/DarkTheme;"
private const val CONTEXT_CLASS = "Landroid/content/Context;"

private fun List<Instruction>.indexOfDroppedMoveResult(reference: MethodReference): Int {
    forEachIndexed { index, instruction ->
        if (instruction.opcode != Opcode.INVOKE_STATIC) return@forEachIndexed
        if ((instruction as ReferenceInstruction).reference != reference) return@forEachIndexed

        val consumer = getOrNull(index + 1) ?: return@forEachIndexed
        if (consumer.opcode != Opcode.INVOKE_STATIC) return@forEachIndexed

        val consumed = (consumer as ReferenceInstruction).reference
        if (consumed is MethodReference &&
            consumed.returnType == "V" &&
            consumed.parameterTypes.singleOrNull()?.toString() == "I"
        ) {
            return index
        }
    }

    return -1
}

@Suppress("unused")
val darkThemePatch = bytecodePatch(
    name = "Dark theme",
    description = "Renders the app's dark theme and adds it to the settings.",
) {
    compatibleWith(AppCompatibilities.CX_FILE_EXPLORER)

    dependsOn(resourceMappingPatch, unlockPremiumPatch, darkThemeResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val themePreference = ThemePreferenceFingerprint.matchSingle().instructionMatches[0]
        val nightMode = themePreference
            .getInstruction<ReferenceInstruction>()
            .reference as MethodReference

        themePreference.getMethodCalled().addInstructions(
            0,
            """
                invoke-static { p0 }, $EXTENSION_CLASS->nightMode($CONTEXT_CLASS)I
                move-result p0
                return p0
            """,
        )

        classDefForEach { classDef ->
            val mutableClassDef by lazy { mutableClassDefBy(classDef) }

            classDef.methods.forEach { method ->
                val instructions = method.implementation?.instructions?.toList() ?: return@forEach
                val index = instructions.indexOfDroppedMoveResult(nightMode)
                if (index < 0) return@forEach

                mutableClassDef.findMutableMethodOf(method).apply {
                    val register = getInstruction<FiveRegisterInstruction>(index + 1).registerC

                    addInstructions(index + 1, "move-result v$register")
                }
            }
        }
    }
}
