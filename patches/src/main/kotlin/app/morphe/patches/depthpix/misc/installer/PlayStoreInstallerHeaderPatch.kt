/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.installer

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val HEADER_SITES = 2

internal val playStoreInstallerHeaderPatch = bytecodePatch {
    execute {
        InstallerHeaderFingerprint.matchAll(HEADER_SITES..HEADER_SITES).forEach { match ->
            val (_, headerCall) = match.instructionMatches
            val valueRegister = headerCall.getInstruction<FiveRegisterInstruction>().registerE
            match.method.addInstruction(headerCall.index, "const-string v$valueRegister, \"$PLAY_STORE_PACKAGE\"")
        }
    }
}
