/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.tabs

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TAB_HISTORY = "Lapp/hxreborn/extension/firefox/TabHistory;"

@Suppress("unused")
val selectPreviousTabOnClosePatch = bytecodePatch(
    name = "Select previous tab on close",
    description = "Selects the last used tab when the selected tab closes.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("selectPreviousTab")

        TabListReduceFingerprint.matchSingle().let { match ->
            val (selected, parent) = match.instructionMatches
            val selectedRegister = selected.getInstruction<OneRegisterInstruction>().registerA
            val parentRegister = parent.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                parent.index + 1,
                """
                    invoke-static { v$parentRegister }, $TAB_HISTORY->selectParent(Z)Z
                    move-result v$parentRegister
                """,
            )
            match.method.addInstructions(
                selected.index + 1,
                "invoke-static { v$selectedRegister }, $TAB_HISTORY->observe(Ljava/lang/String;)V",
            )
        }

        FindNewSelectedTabIdFingerprint.matchSingle().method.let { method ->
            method.addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0, p1, p3 }, $TAB_HISTORY->previous(Ljava/util/ArrayList;ZLjava/util/Set;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :stock
                    return-object v0
                """,
                ExternalLabel("stock", method.getInstruction(0)),
            )
        }
    }
}
