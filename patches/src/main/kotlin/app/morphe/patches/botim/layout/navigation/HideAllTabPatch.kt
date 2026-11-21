/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import app.morphe.patches.shared.misc.extension.botimMainTabsExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/botim/MainTabs;"

@Suppress("unused")
val hideAllTabPatch = patch(
    name = "Hide All tab",
    description = "Hides the All tab from the bottom bar.",
    default = false,
) {
    dependsOn(botimMainTabsExtensionPatch)
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        val (allTabTypeCheck) = AllTabFragmentFactoryFingerprint.matchSingle().instructionMatches
        val allTabType = allTabTypeCheck.instruction.getReference<TypeReference>()!!.type

        mutableClassDefBy(EXTENSION_CLASS).replaceStub(
            "allTabType",
            1,
            """
                const-class v0, $allTabType
                return-object v0
            """,
        )

        TabMapFingerprint.matchSingle().method.apply {
            val returnInstruction = instructions.single { it.opcode == Opcode.RETURN_OBJECT }
            val tabsRegister = (returnInstruction as OneRegisterInstruction).registerA
            addInstructions(
                returnInstruction.location.index,
                """
                    invoke-static { v$tabsRegister }, $EXTENSION_CLASS->withoutAllTab(Ljava/util/Map;)Ljava/util/Map;
                    move-result-object v$tabsRegister
                """,
            )
        }
    }
}
