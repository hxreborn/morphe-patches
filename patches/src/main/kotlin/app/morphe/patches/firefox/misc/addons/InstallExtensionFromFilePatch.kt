/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.addons

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val installExtensionFromFilePatch = bytecodePatch(
    name = "Install extension from file",
    description = "Shows Install extension from file in Settings.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    execute {
        InstallLocalAddonRowFingerprint.matchSingle().let { match ->
            val visible = match.instructionMatches.last()
            val register = visible.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstruction(visible.index + 1, "const/4 v$register, 0x1")
        }
    }
}
