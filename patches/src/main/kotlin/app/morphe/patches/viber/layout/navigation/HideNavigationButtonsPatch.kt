/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.shared.addHiddenIdCheck
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private enum class HomeTab(val label: String, val default: Boolean, vararg val ids: Int) {
    CALLS("Calls", false, 1, 7),
    CHATGPT("ChatGPT", true, 11, 12),
    EXPLORE("Explore", true, 2),
    MARKETPLACE("Marketplace", true, 8),
    PAY("Pay", true, 5),
    SHORTICA("Shortica", true, 10),
}

@Suppress("unused")
val hideNavigationButtonsPatch = bytecodePatch(
    name = "Hide navigation buttons",
    description = "Hides the selected tabs from the bottom navigation bar.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    val hideTabOptions = HomeTab.entries.associateWith { tab ->
        booleanOption(
            key = "hide${tab.name.lowercase().replaceFirstChar(Char::uppercase)}",
            default = tab.default,
            title = "Hide ${tab.label}",
            description = "Hides the ${tab.label} tab.",
        )
    }

    execute {
        val hiddenIds = hideTabOptions.filterValues { it.value == true }.keys.flatMap { it.ids.toList() }
        if (hiddenIds.isEmpty()) return@execute

        val tabVisibilityMethod = BuildHomeTabsFingerprint.matchSingle().instructionMatches.first()
            .instruction.getReference<MethodReference>()!!
        val tabVisibility = tabVisibilityFingerprint(tabVisibilityMethod).matchSingle()
        val isHiddenTab = mutableClassDefBy(tabVisibility.classDef).addHiddenIdCheck("isHiddenTab", hiddenIds)

        tabVisibility.method.apply {
            val resultRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
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
