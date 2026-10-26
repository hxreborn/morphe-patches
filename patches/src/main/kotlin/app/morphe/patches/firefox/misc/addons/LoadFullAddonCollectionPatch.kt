/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/io/github/forkmaintainers/iceraven/components/PagedAMOAddonsProvider.kt
 */
package app.morphe.patches.firefox.misc.addons

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/firefox/AddonCollectionPages;"

@Suppress("unused")
val loadFullAddonCollectionPatch = bytecodePatch(
    name = "Load full add-on collection",
    description = "Lists every add-on in a custom add-on collection, beyond the first 50.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("fullAddonCollection")

        CollectionPageFingerprint.matchSingle().let { match ->
            val body = match.instructionMatches.first { it.instruction.opcode == Opcode.MOVE_RESULT_OBJECT }
            val register = body.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                match.instructionMatches.last().index,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS->appendRemainingPages(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """,
            )
        }
    }
}
