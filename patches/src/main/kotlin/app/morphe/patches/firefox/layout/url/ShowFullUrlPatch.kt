/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/browser/TabPreview.kt
 */
package app.morphe.patches.firefox.layout.url

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val CHAR_SEQUENCE = "Ljava/lang/CharSequence;"
private const val URL_STRING_UTILS = "Lmozilla/components/support/ktx/util/URLStringUtils;"

@Suppress("unused")
val showFullUrlPatch = bytecodePatch(
    name = "Show full URL",
    description = "Shows the full address with https:// and www. in the address bar.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("showFullUrl")

        listOf(ToolbarDisplayUrlFingerprint, TabPreviewDisplayUrlFingerprint).forEach { fingerprint ->
            fingerprint.matchSingle().let { match ->
                val moveResult = match.instructionMatches.first { it.instruction.opcode == Opcode.MOVE_RESULT_OBJECT }
                val callIndex = moveResult.index - 1
                val url = match.method.getInstruction<FiveRegisterInstruction>(callIndex).registerC
                val fullUrl = match.classDef.addStaticMethod(
                    "fullDisplayUrl",
                    listOf(CHAR_SEQUENCE),
                    CHAR_SEQUENCE,
                    2,
                    """
                        invoke-static { }, $PATCH_SETTINGS->showFullUrl()Z
                        move-result v0
                        if-nez v0, :full
                        invoke-static { p0 }, $URL_STRING_UTILS->toDisplayUrl($CHAR_SEQUENCE)$CHAR_SEQUENCE
                        move-result-object p0
                        :full
                        return-object p0
                    """,
                )
                match.method.replaceInstruction(callIndex, "invoke-static { v$url }, $fullUrl")
            }
        }
    }
}
