/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/search/SearchFragmentStore.kt
 */
package app.morphe.patches.firefox.layout.tabsuggestions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableSwitchToTabSuggestionsPatch = bytecodePatch(
    name = "Disable Switch to tab suggestions",
    description = "Removes open tabs from the address bar suggestions.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("noTabSuggestions")

        listOf("showAllSessionSuggestions", "showSessionSuggestionsForCurrentEngine").forEach { field ->
            sessionSuggestionsStateFingerprint(field).matchSingle().let { match ->
                val read = match.instructionMatches.single()
                val flag = read.getInstruction<OneRegisterInstruction>().registerA
                match.method.addInstructions(
                    read.index + 1,
                    """
                        invoke-static { v$flag }, $PATCH_SETTINGS->sessionSuggestions(Z)Z
                        move-result v$flag
                    """,
                )
            }
        }
    }
}
