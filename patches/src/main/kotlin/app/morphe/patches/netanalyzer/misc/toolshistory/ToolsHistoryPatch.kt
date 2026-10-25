/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.toolshistory

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.MaterialIcons
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.ToolItemNormaliserFingerprint
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarIcon
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.propertyFields
import app.morphe.patches.netanalyzer.shared.textButtonSlot
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.netanalyzer.shared.registerAtLaunch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.toJavaClassName
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private val HISTORY_ITEM_PROPERTIES = listOf("address", "favorite")
private const val CONSTRUCTOR = "<init>"
private const val ROUTE_CLASS_MATCH = 1
private const val REGISTRATION_SEPARATOR = ";"

@Suppress("unused")
val toolsHistoryPatch = bytecodePatch(
    name = "Tools history",
    description = "Lists the hosts queried in Tools with their last options, and lets you star or delete them. " +
        "Tapping an entry opens it in Tools.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, patchesMenuPatch)

    execute {
        addToolbarIcon(textButtonSlot("ToolsQueryMenu"), "history", MaterialIcons.HISTORY, "History", "ToolsHistoryActivity")
        addMenuEntry(MenuSection.TOOLS, "Tools History", "ToolsHistoryActivity")

        val itemType = ToolItemNormaliserFingerprint.matchSingle().originalMethod.returnType
        val itemFields = classDefBy(itemType).propertyFields()
        val fieldNames = HISTORY_ITEM_PROPERTIES.joinToString(",") { property ->
            "$property=${itemFields[property] ?: throw PatchException("$itemType has no $property property")}"
        }
        val historyClass = historyRecorderFingerprint(itemType).matchSingle().classDef.type
        val routeClasses = queryRouteNavigationFingerprint(itemType).matchAll().map { navigation ->
            navigation.instructionMatches[ROUTE_CLASS_MATCH].instruction.getReference<TypeReference>()!!.type
        }.distinct()
        val routeClass = routeClasses.singleOrNull()
            ?: throw PatchException("Tools query navigation targets $routeClasses, expected one route class")

        val navigate = NavigateFingerprint.matchSingle()
        val navigatorType = navigate.originalMethod.parameterTypes.first().toString()
        if (navigate.classDef.type != navigatorType) {
            throw PatchException("${navigate.originalMethod} does not navigate its own class $navigatorType")
        }
        val constructor = navigate.classDef.methods.single { it.name == CONSTRUCTOR }
        val thisRegister = constructor.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(constructor)
        val superCall = constructor.indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == CONSTRUCTOR &&
                (this as FiveRegisterInstruction).registerC == thisRegister
        }
        constructor.addInstruction(
            superCall + 1,
            "invoke-static { p0 }, $FEATURES_CLASS->captureNavigator(Ljava/lang/Object;)V",
        )

        val registration = listOf(historyClass.toJavaClassName(), routeClass.toJavaClassName(), navigate.originalMethod.name, fieldNames)
            .joinToString(REGISTRATION_SEPARATOR)
        registerAtLaunch("$FEATURES_CLASS->registerToolsHistory(Ljava/lang/String;)V", registration)
    }
}
