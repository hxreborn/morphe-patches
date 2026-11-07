/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addHiddenIdCheck
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MINIMUM_HOME_TAB_COUNT = 2

private enum class HomeTab(val label: String, val default: Boolean, vararg val ids: Int) {
    CALLS("Calls", false, 1, 7),
    CHATGPT("ChatGPT", true, 11, 12),
    EXPLORE("Explore", true, 2),
    MARKETPLACE("Marketplace", true, 8),
    PAY("Pay", true, 5),
    SHORTICA("Shortica", true, 10),
}

@Suppress("unused")
val hideNavigationButtonsPatch: BytecodePatch = bytecodePatch(
    name = "Hide navigation buttons",
    description = "Hides the selected tabs from the bottom navigation bar.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    val hideTabOptions = HomeTab.entries.associateWith { tab ->
        booleanOption(
            key = "hide${tab.name.lowercase().replaceFirstChar(Char::uppercase)}",
            default = tab.default,
            title = "Hide ${tab.label}",
            description = "Hides the ${tab.label} tab.",
        )
    }

    execute {
        markPatchApplied("hideNavigationButtons", hideNavigationButtonsPatch)
        val hiddenIds = hideTabOptions.filterValues { it.value == true }.keys.flatMap { it.ids.toList() }
        if (hiddenIds.isEmpty()) return@execute

        val (visibilityCall, _) = BuildHomeTabsFingerprint.matchSingle().instructionMatches
        val tabVisibilityMethod = visibilityCall.instruction.getReference<MethodReference>()!!
        val tabVisibility = tabVisibilityFingerprint(tabVisibilityMethod).matchSingle()
        val isHiddenTab = mutableClassDefBy(tabVisibility.classDef).addHiddenIdCheck("isHiddenTab", hiddenIds)

        homeTabCountRangeFingerprint(tabVisibilityMethod.definingClass).matchSingle().let { range ->
            val (minimumCount, _) = range.instructionMatches
            val register = (minimumCount.instruction as OneRegisterInstruction).registerA
            range.method.replaceInstruction(minimumCount.index, "const/4 v$register, $MINIMUM_HOME_TAB_COUNT")
        }
        InflateHomeTabsFingerprint.matchSingle().let { inflation ->
            val (_, minimumCount, _) = inflation.instructionMatches
            val register = (minimumCount.instruction as OneRegisterInstruction).registerA
            inflation.method.replaceInstruction(minimumCount.index, "const/4 v$register, $MINIMUM_HOME_TAB_COUNT")
        }

        tabVisibility.method.apply {
            val resultRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $VIBER_SETTINGS_CLASS->hideNavigationButtons()Z
                    move-result v$resultRegister
                    if-eqz v$resultRegister, :show
                    invoke-static { p1 }, $isHiddenTab
                    move-result v$resultRegister
                    if-eqz v$resultRegister, :show
                    const/4 v$resultRegister, 0x0
                    return v$resultRegister
                """,
                ExternalLabel("show", getInstruction(0)),
            )
        }
    }
}
