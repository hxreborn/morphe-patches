/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/build.gradle
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

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/firefox/AddonCollection;"

@Suppress("unused")
val extendedAddonListPatch = bytecodePatch(
    name = "Extended add-on list",
    description = "Lists more Android-compatible add-ons than the recommended list.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("extendedAddonList")

        listOf(
            DefaultCollectionUserFingerprint to "$EXTENSION_CLASS->user(Ljava/lang/String;)Ljava/lang/String;",
            DefaultCollectionNameFingerprint to "$EXTENSION_CLASS->name(Ljava/lang/String;)Ljava/lang/String;",
        ).forEach { (fingerprint, call) ->
            fingerprint.matchSingle().let { match ->
                val constant = match.instructionMatches.first { it.instruction.opcode == Opcode.CONST_STRING }
                val register = constant.getInstruction<OneRegisterInstruction>().registerA
                match.method.addInstructions(
                    constant.index + 1,
                    """
                        invoke-static { v$register }, $call
                        move-result-object v$register
                    """,
                )
            }
        }
    }
}
