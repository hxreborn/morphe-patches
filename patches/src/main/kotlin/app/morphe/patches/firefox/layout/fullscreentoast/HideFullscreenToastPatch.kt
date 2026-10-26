/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/browser/BaseBrowserFragment.kt
 */
package app.morphe.patches.firefox.layout.fullscreentoast

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val hideFullscreenToastPatch = bytecodePatch(
    name = "Hide fullscreen toast",
    description = "Hides the exit fullscreen hint shown when a video goes fullscreen.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("hideFullscreenToast")

        FullscreenToastFingerprint.matchSingle().let { match ->
            val show = match.instructionMatches.last()
            val toast = show.getInstruction<FiveRegisterInstruction>().registerC
            val showToast = match.classDef.addStaticMethod(
                "showFullscreenToast",
                listOf(TOAST),
                "V",
                2,
                """
                    invoke-static { }, $PATCH_SETTINGS->hideFullscreenToast()Z
                    move-result v0
                    if-nez v0, :hidden
                    invoke-virtual { p0 }, $TOAST->show()V
                    :hidden
                    return-void
                """,
            )
            match.method.replaceInstruction(show.index, "invoke-static { v$toast }, $showToast")
        }
    }
}
