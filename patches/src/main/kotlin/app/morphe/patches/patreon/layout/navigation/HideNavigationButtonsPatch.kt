/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.util.StringUtils.escapeString

private enum class NavigationTab(val label: String, val tabName: String) {
    FEED("Feed", "MemberModeHomeTab"),
    LIBRARY("Library", "MemberModeLibraryTab"),
    QUIP("Quip", "MemberModeCreateQuickPostTab"),
    COMMUNITY("Community", "MemberModeChatTab"),
    NOTIFICATIONS("Notifications", "MemberModeNotificationsTab"),
}

private const val NAVIGATION_BUTTONS_CLASS = "${PATREON_EXTENSION_PACKAGE}NavigationButtons;"

@Suppress("unused")
val hideNavigationButtonsPatch: BytecodePatch = bytecodePatch(
    name = "Hide navigation buttons",
    description = "Removes chosen tabs from the bottom bar.",
    default = false,
) {
    compatibleWith(AppCompatibilities.PATREON)

    val hideTabOptions = NavigationTab.entries.associateWith { tab ->
        booleanOption(
            key = "hide${tab.name.lowercase().replaceFirstChar(Char::uppercase)}",
            default = false,
            title = "Hide ${tab.label}",
            description = "Removes ${tab.label} from the bottom bar.",
        )
    }

    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(NAVIGATION_BUTTONS_CLASS, hideNavigationButtonsPatch, withDescription = false)

        val methodsByName = mutableClassDefBy(NAVIGATION_BUTTONS_CLASS).methods.associateBy { it.name }
        hideTabOptions.forEach { (tab, option) ->
            methodsByName.getValue("${tab.name.lowercase()}Description")
                .returnEarly(escapeString(checkNotNull(option.description)))
        }

        val defaultHiddenTabs = hideTabOptions.filterValues { it.value == true }.keys
        mutableClassDefBy(NAVIGATION_BUTTONS_CLASS).methods.single { it.name == "defaultHiddenTabs" }
            .returnEarly(defaultHiddenTabs.joinToString(",") { it.tabName })

        val tabClasses = NavigationTab.entries.associateWith { bottomTabToStringFingerprint(it.tabName).classDef }
        val tabTypes = tabClasses.values.map { it.type }.toSet()

        NavigationBarTabsFingerprint.method.apply {
            instructions.withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.SGET_OBJECT &&
                        instruction.getReference<FieldReference>()?.type in tabTypes
                }
                .map { it.index }
                .sortedDescending()
                .forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    val tabType = getInstruction(index).getReference<FieldReference>()!!.type
                    addInstructions(
                        index + 1,
                        """
                            invoke-static/range { v$register .. v$register }, $NAVIGATION_BUTTONS_CLASS->visibleTab(Ljava/lang/Object;)Ljava/lang/Object;
                            move-result-object v$register
                            check-cast v$register, $tabType
                        """,
                    )
                }
        }

        fun tabInstance(tab: NavigationTab) = tabClasses.getValue(tab).let { tabClass ->
            tabClass.fields.single { AccessFlags.STATIC.isSet(it.accessFlags) && it.type == tabClass.type }
        }

        val startRoute = startTabRouteFingerprint(
            NavigationBarSpecToStringFingerprint.classDef.type,
            tabClasses.getValue(NavigationTab.FEED).type,
        )
        val (_, routeCall, routeResult) = startRoute.instructionMatches
        val routeGetter = routeCall.instruction.getReference<MethodReference>()!!
        val routeIndex = routeResult.index
        startRoute.method.apply {
            val register = getInstruction<OneRegisterInstruction>(routeIndex).registerA
            val flag = findFreeRegister(routeIndex + 1, register)
            fun routeOf(tab: NavigationTab) = tabClasses.getValue(tab).type.let { type ->
                """
                    sget-object v$register, ${descriptor(tabInstance(tab))}
                    invoke-virtual { v$register }, $type->${routeGetter.name}()${routeGetter.returnType}
                    move-result-object v$register
                """
            }
            addInstructionsWithLabels(
                routeIndex + 1,
                """
                    invoke-static { }, $NAVIGATION_BUTTONS_CLASS->startsOnLibrary()Z
                    move-result v$flag
                    if-eqz v$flag, :check_community
                    ${routeOf(NavigationTab.LIBRARY)}
                    goto :start_route
                    :check_community
                    invoke-static { }, $NAVIGATION_BUTTONS_CLASS->startsOnCommunity()Z
                    move-result v$flag
                    if-eqz v$flag, :start_route
                    ${routeOf(NavigationTab.COMMUNITY)}
                """,
                ExternalLabel("start_route", getInstruction(routeIndex + 1)),
            )
        }
    }
}
