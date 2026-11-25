/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import app.morphe.patches.shared.misc.extension.outlookHideNavigationButtonsExtensionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val NAVIGATION_TABS_CLASS = "Lapp/hxreborn/extension/outlook/navigation/NavigationTabs;"

@Suppress("unused")
val hideNavigationButtonsPatch: Patch = patch(
    name = "Hide navigation buttons",
    description = "Hides Calendar, Apps or the whole bottom bar. Choose which in the patches menu.",
) {
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(
        outlookAppliedPatchesExtensionPatch,
        fixSignInPatch,
        patchesSettingsPatch,
        outlookHideNavigationButtonsExtensionPatch,
    )

    execute {
        markPatchApplied("hideNavigationButtons", hideNavigationButtonsPatch)
        mutableClassDefBy(NAVIGATION_TABS_CLASS).apply {
            replaceStub(
                name = "isCalendar",
                registers = 2,
                smali = """
                    instance-of v0, p0, ${classDefBy(CALENDAR_CONTRIBUTION_CLASS).type}
                    return v0
                """,
            )
            replaceResourceIdStubs(ResourceType.ID, mapOf("appsTabId" to "menu_more"))
        }

        AppGroupsFingerprint.match().let { appGroups ->
            val (appGroupsConstructorCall) = appGroups.instructionMatches
            val constructor = appGroups.method.getInstruction<FiveRegisterInstruction>(appGroupsConstructorCall.index)
            val pinnedApps = constructor.registerD
            val moreApps = constructor.registerE
            appGroups.method.apply {
                val appManager = getFreeRegisterProvider(appGroupsConstructorCall.index, 1).getFreeRegister4Bit()
                addInstructions(
                    appGroupsConstructorCall.index,
                    """
                        invoke-static {v$pinnedApps, v$moreApps}, $NAVIGATION_TABS_CLASS->moreApps(Ljava/util/List;Ljava/util/List;)Ljava/util/List;
                        move-result-object v$moreApps
                        move-object/from16 v$appManager, p0
                        invoke-static {v$appManager, v$pinnedApps}, $NAVIGATION_TABS_CLASS->pinnedApps(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
                        move-result-object v$pinnedApps
                    """,
                )
            }
        }

        SaveAppGroupsFingerprint.method.apply {
            val registers = getFreeRegisterProvider(index = 0, numberOfFreeRegistersNeeded = 4)
            val pinnedApps = registers.getFreeRegister4Bit()
            val moreApps = registers.getFreeRegister4Bit()
            val restoreIndex = registers.getFreeRegister4Bit()
            val savedMoreApps = registers.getFreeRegister4Bit()
            addInstructions(
                0,
                """
                    invoke-virtual {p1}, $APP_GROUPS_CLASS->getPinnedApps()Ljava/util/List;
                    move-result-object v$pinnedApps
                    invoke-virtual {p1}, $APP_GROUPS_CLASS->getMoreApps()Ljava/util/List;
                    move-result-object v$moreApps
                    invoke-static {p0, v$pinnedApps, v$moreApps}, $NAVIGATION_TABS_CLASS->calendarRestoreIndex(Ljava/lang/Object;Ljava/util/List;Ljava/util/List;)I
                    move-result v$restoreIndex
                    invoke-static {v$restoreIndex, v$moreApps}, $NAVIGATION_TABS_CLASS->savedMoreApps(ILjava/util/List;)Ljava/util/List;
                    move-result-object v$savedMoreApps
                    invoke-static {v$restoreIndex, v$pinnedApps, v$moreApps}, $NAVIGATION_TABS_CLASS->savedPinnedApps(ILjava/util/List;Ljava/util/List;)Ljava/util/List;
                    move-result-object v$pinnedApps
                    new-instance p1, $APP_GROUPS_CLASS
                    invoke-direct {p1, v$pinnedApps, v$savedMoreApps}, $APP_GROUPS_CLASS-><init>(Ljava/util/List;Ljava/util/List;)V
                """,
            )
        }

        LegacyTabsAddedFingerprint.match().let { tabsAdded ->
            val (menuViewRead) = tabsAdded.instructionMatches
            val menuViewField = menuViewRead.getInstruction<ReferenceInstruction>().reference as FieldReference
            tabsAdded.method.apply {
                val tabsReturn = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
                val menu = getFreeRegisterProvider(tabsReturn, 1).getFreeRegister4Bit()
                addInstructions(
                    tabsReturn,
                    """
                        move-object/from16 v$menu, p0
                        iget-object v$menu, v$menu, ${descriptor(menuViewField)}
                        invoke-virtual {v$menu}, $MENU_VIEW_CLASS->getMenu()Landroid/view/Menu;
                        move-result-object v$menu
                        invoke-static {v$menu}, $NAVIGATION_TABS_CLASS->hideAppsTab(Landroid/view/Menu;)V
                    """,
                )
            }
        }

        ComposeAppsTabFingerprint.match().let { appsTab ->
            val (_, _, noMoreAppsResult) = appsTab.instructionMatches
            val noMoreApps = noMoreAppsResult.getInstruction<OneRegisterInstruction>().registerA
            appsTab.method.addInstructions(
                noMoreAppsResult.index + 1,
                """
                    invoke-static {v$noMoreApps}, $NAVIGATION_TABS_CLASS->isAppsTabHidden(Z)Z
                    move-result v$noMoreApps
                """,
            )
        }

        LegacyNavigationBarVisibilityFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $NAVIGATION_TABS_CLASS->navigationBarVisible(Z)Z
                move-result p1
            """,
        )

        ComposeNavigationSuiteEffectFingerprint.match().let { effect ->
            val (_, _, visibleResult, suiteStateRead) = effect.instructionMatches
            val visible = visibleResult.getInstruction<OneRegisterInstruction>().registerA
            val suiteStateField = suiteStateRead.getInstruction<ReferenceInstruction>().reference as FieldReference
            effect.method.apply {
                val suiteState = getFreeRegisterProvider(visibleResult.index + 1, 1).getFreeRegister4Bit()
                addInstructions(
                    visibleResult.index + 1,
                    """
                        iget-object v$suiteState, p0, ${descriptor(suiteStateField)}
                        invoke-static {v$suiteState, v$visible}, $NAVIGATION_TABS_CLASS->navigationSuiteVisible(Ljava/lang/Object;Z)Z
                        move-result v$visible
                    """,
                )
            }
        }

        ComposeNavigationSuiteKeyFingerprint.match().let { key ->
            val (_, visibleResult, _, effectConstructor, navigationRailCall) = key.instructionMatches
            val visible = visibleResult.getInstruction<OneRegisterInstruction>().registerA
            val suiteState = effectConstructor.getInstruction<FiveRegisterInstruction>().registerD
            val navigationRailInstruction = navigationRailCall.getInstruction<ReferenceInstruction>()
            val isNavigationRailMethod = descriptor(navigationRailInstruction.reference as MethodReference)
            val suiteType = firstRegister(navigationRailInstruction)
            key.method.apply {
                val navigationRail = getFreeRegisterProvider(visibleResult.index + 1, 1).getFreeRegister4Bit()
                addInstructions(
                    visibleResult.index + 1,
                    """
                        invoke-static/range {v$suiteType .. v$suiteType}, $isNavigationRailMethod
                        move-result v$navigationRail
                        invoke-static {v$suiteState, v$navigationRail}, $NAVIGATION_TABS_CLASS->setNavigationRail(Ljava/lang/Object;Z)V
                        invoke-static {v$suiteState, v$visible}, $NAVIGATION_TABS_CLASS->navigationSuiteVisible(Ljava/lang/Object;Z)Z
                        move-result v$visible
                    """,
                )
            }
        }

        ComposeDynamicActionBarFingerprint.match().let { actionBar ->
            val (_, _, _, _, visibleResult) = actionBar.instructionMatches
            val visible = visibleResult.getInstruction<OneRegisterInstruction>().registerA
            actionBar.method.addInstructions(
                visibleResult.index + 1,
                """
                    invoke-static/range {v$visible .. v$visible}, $NAVIGATION_TABS_CLASS->navigationBarVisible(Z)Z
                    move-result v$visible
                """,
            )
        }
    }
}

private fun firstRegister(call: ReferenceInstruction) = when (call) {
    is RegisterRangeInstruction -> call.startRegister
    is FiveRegisterInstruction -> call.registerC
    else -> throw PatchException("Unexpected call format ${call.opcode}")
}
