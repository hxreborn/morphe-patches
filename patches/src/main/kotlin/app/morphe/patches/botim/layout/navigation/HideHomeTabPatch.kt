/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import app.morphe.patches.shared.misc.extension.botimMainTabsExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/botim/MainTabs;"

@Suppress("unused")
val hideHomeTabPatch = patch(
    name = "Hide Home tab",
    description = "Hides the Home tab from the bottom bar and opens the app on Calls.",
    default = false,
) {
    dependsOn(botimMainTabsExtensionPatch)
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        val factory = TabFragmentFactoryFingerprint.match()
        val (homeTypeMatch, _, callsTypeMatch) = factory.instructionMatches
        val homeTabType = homeTypeMatch.instruction.getReference<TypeReference>()!!.type
        val callsTabType = callsTypeMatch.instruction.getReference<TypeReference>()!!.type

        val homeTabGetter = factory.classDef.tabGetter(homeTabType)
        val callsTabGetter = factory.classDef.tabGetter(callsTabType)

        SelectedTabInitializerFingerprint.method.apply {
            val homeTabCall = instructions.single { instruction ->
                val call = instruction.getReference<MethodReference>()
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    call?.definingClass == MAIN_ACTIVITY &&
                    call.name == homeTabGetter.name
            }
            replaceInstruction(
                homeTabCall.location.index,
                "invoke-virtual { p0 }, $MAIN_ACTIVITY->${callsTabGetter.name}()$callsTabType",
            )
        }

        TabMapFingerprint.method.apply {
            val returnInstruction = instructions.single { it.opcode == Opcode.RETURN_OBJECT }
            val tabsRegister = (returnInstruction as OneRegisterInstruction).registerA
            addInstructions(
                returnInstruction.location.index,
                """
                    invoke-virtual { p0 }, $MAIN_ACTIVITY->${homeTabGetter.name}()$homeTabType
                    move-result-object p0
                    invoke-static { v$tabsRegister, p0 }, $EXTENSION_CLASS->withoutTab(Ljava/util/Map;Ljava/lang/Object;)Ljava/util/Map;
                    move-result-object v$tabsRegister
                """,
            )
        }
    }
}

private fun ClassDef.tabGetter(tabType: String) = methods.singleOrNull {
    it.returnType == tabType && it.parameterTypes.isEmpty()
} ?: throw PatchException("Expected one $tabType getter in $type")
