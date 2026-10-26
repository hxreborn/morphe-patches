/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/search/BrowserToolbarSearchMiddleware.kt
 */
package app.morphe.patches.firefox.layout.qrscanner

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val hideQrScannerButtonPatch = bytecodePatch(
    name = "Hide QR scanner button",
    description = "Hides the QR code scanner button in the address bar.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("hideQrScanner")

        QrScannerButtonFingerprint.matchSingle().let { match ->
            val add = match.instructionMatches.last()
            val button = add.getInstruction<FiveRegisterInstruction>()
            val addUnlessHidden = match.classDef.addStaticMethod(
                "addQrScannerButton",
                listOf(LIST_BUILDER, "Ljava/lang/Object;"),
                "Z",
                3,
                """
                    invoke-static { }, $PATCH_SETTINGS->hideQrScanner()Z
                    move-result v0
                    if-eqz v0, :add
                    const/4 v0, 0x0
                    return v0
                    :add
                    invoke-virtual { p0, p1 }, $LIST_BUILDER->add(Ljava/lang/Object;)Z
                    move-result v0
                    return v0
                """,
            )
            match.method.replaceInstruction(
                add.index,
                "invoke-static { v${button.registerC}, v${button.registerD} }, $addUnlessHidden",
            )
        }
    }
}
